import type { Octokit } from "@octokit/rest";
import { Issue, PullRequest } from '../types';

let octokit: Octokit | null = null;
let currentToken: string | null = null;
const commentsCache = new Map<string, { timestamp: string; data: any[] }>();
const statsCache = new Map<string, { timestamp: number; count: number }>();
const STATS_CACHE_TTL = 60000; // 60 seconds

const decodeBase64 = (value: string) => {
  if (typeof atob === "function") {
    try {
      return decodeURIComponent(escape(atob(value)));
    } catch {
      return atob(value);
    }
  }
  return Buffer.from(value, "base64").toString("utf-8");
};

const encodeBase64 = (value: string) => {
  if (typeof btoa === "function") {
    try {
      return btoa(unescape(encodeURIComponent(value)));
    } catch {
      return btoa(value);
    }
  }
  return Buffer.from(value, "utf-8").toString("base64");
};

const getOctokit = async (token?: string): Promise<Octokit> => {
  if (token) {
    const { Octokit } = await import("@octokit/rest");
    return new Octokit({ auth: token });
  }
  if (octokit) return octokit;
  if (!currentToken) throw new Error("GitHub service not initialized. Please configure your token.");

  const { Octokit } = await import("@octokit/rest");
  octokit = new Octokit({ auth: currentToken });
  return octokit;
};

const mapPriority = (labels: any[]): Issue['priority'] => {
  const names = labels.map((l: any) => (l.name || '').toLowerCase());
  if (names.some(n => n === 'critical' || n.includes('p0') || n.includes('sev: critical') || n.includes('severity: critical'))) return 'critical';
  if (names.some(n => n === 'high' || n.includes('p1') || n.includes('priority: high') || n.includes('severity: high'))) return 'high';
  if (names.some(n => n === 'medium' || n.includes('p2') || n.includes('priority: medium') || n.includes('severity: medium'))) return 'medium';
  if (names.some(n => n === 'low' || n.includes('p3') || n.includes('priority: low') || n.includes('severity: low') || n.includes('minor'))) return 'low';
  return 'medium';
};

export const githubService = {
  initialize: (token: string) => {
    currentToken = token;
    octokit = null; // Force recreation with new token on next use
    statsCache.clear(); // Clear stats cache when switching tokens/users
  },

  getOwnerType: async (owner: string): Promise<'User' | 'Organization' | null> => {
    const api = await getOctokit();
    try {
      const { data } = await api.users.getByUsername({ username: owner });
      return data.type === 'Organization' ? 'Organization' : 'User';
    } catch (e) {
      console.error("Failed to fetch owner type", e);
      return null;
    }
  },

  getIssueComments: async (owner: string, repo: string, issueNumber: number, lastUpdated?: string) => {
    const api = await getOctokit();

    const cacheKey = `${owner}/${repo}/${issueNumber}`;

    // Check cache if lastUpdated is provided
    if (lastUpdated) {
      const cached = commentsCache.get(cacheKey);
      if (cached && cached.timestamp === lastUpdated) {
        return cached.data;
      }
    }

    const data = await api.paginate(api.issues.listComments, {
      owner,
      repo,
      issue_number: issueNumber,
      per_page: 100
    });

    // Update cache
    if (lastUpdated) {
      if (commentsCache.size > 500) {
        commentsCache.clear(); // Simple eviction strategy to prevent memory leaks
      }
      commentsCache.set(cacheKey, { timestamp: lastUpdated, data });
    }

    return data;
  },

  getIssues: async (owner: string, repo: string, state: 'open' | 'closed' = 'open'): Promise<Issue[]> => {
    const api = await getOctokit();

    try {
      const response = await api.paginate(api.issues.listForRepo, {
        owner,
        repo,
        state,
        sort: 'updated',
        direction: 'desc',
        per_page: 100
      });

      // Filter out PRs as they are returned in issues endpoint too
      return response
        .filter((i: any) => !i.pull_request)
        .map((i: any) => ({
          id: i.id,
          number: i.number,
          title: i.title,
          description: i.body || '',
          state: i.state,
          priority: mapPriority(i.labels),
          labels: i.labels.map((l: any) => l.name),
          type: i.labels.find((l: any) => l.name === 'bug' || l.name === 'feature' || l.name === 'ui')?.name || 'bug',
          createdAt: i.created_at,
          updatedAt: i.updated_at,
          commentsCount: i.comments,
          reporter: {
            name: i.user.login,
            avatar: i.user.avatar_url
          },
          comments: [] // Would need separate fetch, keeping empty for list view performance
        }));
    } catch (e) {
      console.error("Failed to fetch issues", e);
      return [];
    }
  },

  getPullRequests: async (owner: string, repo: string, token?: string): Promise<PullRequest[]> => {
    const api = await getOctokit(token);

    try {
      const response = await api.paginate(api.pulls.list, {
        owner,
        repo,
        state: 'open',
        sort: 'updated',
        direction: 'desc',
        per_page: 100
      });

      return response.map((pr: any) => ({
        id: pr.id,
        number: pr.number,
        title: pr.title,
        branch: pr.head.ref,
        targetBranch: pr.base.ref,
        author: {
          name: pr.user.login,
          avatar: pr.user.avatar_url
        },
        hasConflicts: false, // Default, client updates this via detailed check if needed
        isDraft: pr.draft,
        status: pr.draft ? 'draft' : 'open',
        filesChanged: 0
      }));
    } catch (e) {
      console.error("Failed to fetch PRs", e);
      return [];
    }
  },

  getIssue: async (owner: string, repo: string, issueNumber: number): Promise<Issue> => {
    const api = await getOctokit();
    const { data } = await api.issues.get({
      owner,
      repo,
      issue_number: issueNumber
    });
    return {
      id: data.id,
      number: data.number,
      title: data.title,
      description: data.body || '',
      state: data.state,
      priority: mapPriority(data.labels),
      labels: (data.labels || []).map((l: any) => l.name),
      type: (data.labels || []).find((l: any) => l.name === 'bug' || l.name === 'feature' || l.name === 'ui')?.name || 'bug',
      createdAt: data.created_at,
      updatedAt: data.updated_at,
      commentsCount: data.comments,
      reporter: {
        name: data.user?.login || '',
        avatar: data.user?.avatar_url || ''
      },
      comments: []
    };
  },

  // Get full details for a single PR, including mergeable state
  getPullRequest: async (owner: string, repo: string, pullNumber: number) => {
      const api = await getOctokit();
      const { data } = await api.pulls.get({
          owner,
          repo,
          pull_number: pullNumber
      });
      return data;
  },

  getPullRequestFiles: async (owner: string, repo: string, pullNumber: number) => {
      const api = await getOctokit();
      const files = await api.paginate(api.pulls.listFiles, {
          owner,
          repo,
          pull_number: pullNumber,
          per_page: 100
      });
      return files.map((file: any) => ({
          filename: file.filename,
          status: file.status,
          additions: file.additions,
          deletions: file.deletions,
          changes: file.changes,
          patch: file.patch
      }));
  },

  getFileContent: async (owner: string, repo: string, path: string, ref: string) => {
      const api = await getOctokit();
      const { data } = await api.repos.getContent({
          owner,
          repo,
          path,
          ref
      });
      if (Array.isArray(data) || (data as any).type !== "file") {
          throw new Error("Unsupported content type for file resolution.");
      }
      const file = data as any;
      return {
          content: decodeBase64(file.content || ""),
          sha: file.sha,
          path: file.path,
          encoding: file.encoding
      };
  },

  updateFileContent: async (
      owner: string,
      repo: string,
      path: string,
      branch: string,
      content: string,
      sha: string,
      message: string
  ) => {
      const api = await getOctokit();
      await api.repos.createOrUpdateFileContents({
          owner,
          repo,
          path,
          branch,
          sha,
          message,
          content: encodeBase64(content)
      });
      return true;
  },

  createIssue: async (repoId: string, issue: Partial<Issue>, owner: string, repoName: string) => {
    const api = await getOctokit();
    
    const response = await api.issues.create({
      owner: owner,
      repo: repoName,
      title: issue.title || '',
      body: issue.description,
      labels: issue.labels
    });
    
    return response.data;
  },

  addComment: async (owner: string, repo: string, issueNumber: number, body: string) => {
    const api = await getOctokit();
    await api.issues.createComment({
      owner,
      repo,
      issue_number: issueNumber,
      body
    });
    return true;
  },

  updateIssueStatus: async (owner: string, repo: string, issueNumber: number, state: 'open' | 'closed') => {
    const api = await getOctokit();
    await api.issues.update({
      owner,
      repo,
      issue_number: issueNumber,
      state
    });
    return true;
  },
  
  mergePR: async (owner: string, repo: string, pullNumber: number) => {
    const api = await getOctokit();
    await api.pulls.merge({
        owner,
        repo,
        pull_number: pullNumber
    });
    return true;
  },

  denyPR: async (owner: string, repo: string, pullNumber: number) => {
      const api = await getOctokit();
      await api.pulls.update({
          owner,
          repo,
          pull_number: pullNumber,
          state: 'closed'
      });
      return true;
  },

  updatePR: async (owner: string, repo: string, pullNumber: number, updates: any) => {
      const api = await getOctokit();

      const { isDraft, ...restUpdates } = updates;

      if (isDraft !== undefined) {
          const { data: pr } = await api.pulls.get({
              owner,
              repo,
              pull_number: pullNumber
          });

          // Toggle draft status using GraphQL mutations
          const mutation = isDraft
              ? `mutation($id: ID!) { convertPullRequestToDraft(input: {pullRequestId: $id}) { clientMutationId } }`
              : `mutation($id: ID!) { markPullRequestReadyForReview(input: {pullRequestId: $id}) { clientMutationId } }`;

          await api.request('POST /graphql', {
              query: mutation,
              variables: {
                  id: pr.node_id
              }
          });
      }

      // Apply other REST updates if any
      if (Object.keys(restUpdates).length > 0) {
          await api.pulls.update({
              owner,
              repo,
              pull_number: pullNumber,
              ...restUpdates
          });
      }

      return true;
  },

  markReadyForReview: async (owner: string, repo: string, pullNumber: number) => {
      const api = await getOctokit();
      const { data: pr } = await api.pulls.get({
          owner,
          repo,
          pull_number: pullNumber
      });

      if (!pr.draft) {
          return true;
      }

      const mutation = `mutation($id: ID!) { markPullRequestReadyForReview(input: {pullRequestId: $id}) { clientMutationId } }`;
      await api.request('POST /graphql', {
          query: mutation,
          variables: { id: pr.node_id }
      });
      return true;
  },

  addReviewer: async (owner: string, repo: string, pullNumber: number, reviewer: string) => {
     const api = await getOctokit();
     await api.pulls.requestReviewers({
         owner,
         repo,
         pull_number: pullNumber,
         reviewers: [reviewer] 
     });
     return true;
  },

  approvePR: async (owner: string, repo: string, pullNumber: number) => {
     const api = await getOctokit();
     await api.pulls.createReview({
         owner,
         repo,
         pull_number: pullNumber,
         event: 'APPROVE'
     });
     return true;
  },

  // Try to update branch with base to resolve simple behind-head conflicts
  updateBranch: async (owner: string, repo: string, pullNumber: number) => {
      const api = await getOctokit();
      try {
          await api.pulls.updateBranch({
              owner,
              repo,
              pull_number: pullNumber
          });
          return true;
      } catch (e) {
          console.error("Auto-resolve (update branch) failed", e);
          return false;
      }
  },

  getOpenIssueCount: async (owner: string, repo: string, token?: string): Promise<number> => {
    const cacheKey = `issue-${owner}-${repo}`;
    const cached = statsCache.get(cacheKey);
    if (cached && Date.now() - cached.timestamp < STATS_CACHE_TTL) {
      return cached.count;
    }

    const api = await getOctokit(token);
    const response = await api.search.issuesAndPullRequests({
      q: `repo:${owner}/${repo} is:issue is:open`,
    });

    statsCache.set(cacheKey, { timestamp: Date.now(), count: response.data.total_count });
    return response.data.total_count;
  },

  getOpenPullRequestCount: async (owner: string, repo: string, token?: string): Promise<number> => {
    const cacheKey = `pr-${owner}-${repo}`;
    const cached = statsCache.get(cacheKey);
    if (cached && Date.now() - cached.timestamp < STATS_CACHE_TTL) {
      return cached.count;
    }

    const api = await getOctokit(token);
    const response = await api.search.issuesAndPullRequests({
      q: `repo:${owner}/${repo} is:pr is:open`,
    });

    statsCache.set(cacheKey, { timestamp: Date.now(), count: response.data.total_count });
    return response.data.total_count;
  }
};
