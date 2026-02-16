import type { Octokit } from "@octokit/rest";
import { Issue, PullRequest, Comment } from '../types';

let octokit: Octokit | null = null;
let currentToken: string | null = null;
const commentsCache = new Map<string, { timestamp: string; data: Comment[] }>();
const statsCache = new Map<string, { timestamp: number; count: number }>();
const repoStatsCache = new Map<string, { timestamp: number; data: { issues: number; prs: number } }>();
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

const getIssueComments = async (owner: string, repo: string, issueNumber: number, lastUpdated?: string): Promise<Comment[]> => {
  const api = await getOctokit();

  const cacheKey = `${owner}/${repo}/${issueNumber}`;

  // Check cache if lastUpdated is provided
  if (lastUpdated) {
    const cached = commentsCache.get(cacheKey);
    if (cached && cached.timestamp === lastUpdated) {
      return cached.data;
    }
  }

  const response = await api.paginate(api.issues.listComments, {
    owner,
    repo,
    issue_number: issueNumber,
    per_page: 100
  });

  const data: Comment[] = response.map((c: any) => ({
    id: String(c.id),
    text: c.body || '',
    buildNumber: undefined
  }));

  // Update cache
  if (lastUpdated) {
    if (commentsCache.size > 500) {
      commentsCache.clear(); // Simple eviction strategy to prevent memory leaks
    }
    commentsCache.set(cacheKey, { timestamp: lastUpdated, data });
  }

  return data;
};

const getIssuesWithCommentsGraphQL = async (owner: string, repo: string, state: 'open' | 'closed'): Promise<Issue[]> => {
  const api = await getOctokit();
  const allIssues: Issue[] = [];
  let hasNextPage = true;
  let cursor: string | null = null;
  let loopCount = 0;
  const MAX_LOOPS = 20;
  const stateParam = state === 'open' ? 'OPEN' : 'CLOSED';

  while (hasNextPage && loopCount < MAX_LOOPS) {
    loopCount++;
    const response: any = await api.request('POST /graphql', {
      query: `
        query ($owner: String!, $repo: String!, $cursor: String, $states: [IssueState!]) {
          repository(owner: $owner, name: $repo) {
            issues(first: 50, states: $states, orderBy: {field: UPDATED_AT, direction: DESC}, after: $cursor) {
              pageInfo {
                hasNextPage
                endCursor
              }
              nodes {
                databaseId
                number
                title
                body
                state
                createdAt
                updatedAt
                author {
                  login
                  avatarUrl
                }
                labels(first: 10) {
                  nodes {
                    name
                  }
                }
                comments(last: 50) {
                  totalCount
                  nodes {
                    databaseId
                    body
                  }
                }
              }
            }
          }
        }
      `,
      variables: {
        owner,
        repo,
        cursor,
        states: [stateParam]
      }
    });

    if (response.data.errors) {
      console.error("GraphQL Errors while fetching issues:", response.data.errors);
      if (!response.data.data) break;
    }

    const data = response.data.data?.repository?.issues;
    if (!data) break;

    const mapped = data.nodes.map((i: any) => ({
      id: i.databaseId,
      number: i.number,
      title: i.title,
      description: i.body || '',
      state: i.state.toLowerCase(),
      priority: mapPriority(i.labels.nodes),
      labels: i.labels.nodes.map((l: any) => l.name),
      type: i.labels.nodes.find((l: any) => l.name === 'bug' || l.name === 'feature' || l.name === 'ui')?.name || 'bug',
      createdAt: i.createdAt,
      updatedAt: i.updatedAt,
      commentsCount: i.comments.totalCount,
      reporter: {
        name: i.author?.login || 'Unknown',
        avatar: i.author?.avatarUrl || ''
      },
      comments: i.comments.nodes.map((c: any) => ({
        id: String(c.databaseId),
        text: c.body || '',
        buildNumber: undefined
      }))
    }));

    allIssues.push(...mapped);
    hasNextPage = data.pageInfo.hasNextPage;
    cursor = data.pageInfo.endCursor;
  }

  return allIssues;
};

export const githubService = {
  initialize: (token: string) => {
    currentToken = token;
    octokit = null; // Force recreation with new token on next use
    statsCache.clear(); // Clear stats cache when switching tokens/users
    repoStatsCache.clear();
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

  getIssueComments,

  getIssues: async (owner: string, repo: string, state: 'open' | 'closed' = 'open', includeComments = false): Promise<Issue[]> => {
    const api = await getOctokit();

    try {
      if (includeComments) {
        // Optimized path using GraphQL to fetch issues and comments in one go
        return await getIssuesWithCommentsGraphQL(owner, repo, state);
      }

      const response = await api.paginate(api.issues.listForRepo, {
        owner,
        repo,
        state,
        sort: 'updated',
        direction: 'desc',
        per_page: 100
      });

      // Filter out PRs as they are returned in issues endpoint too
      const issues: Issue[] = response
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
          comments: []
        }));

      if (includeComments) {
        await Promise.all(issues.map(async (issue) => {
          if (issue.commentsCount > 0) {
            issue.comments = await getIssueComments(owner, repo, issue.number, issue.updatedAt);
          }
        }));
      }

      return issues;
    } catch (e) {
      console.error("Failed to fetch issues", e);
      throw e;
    }
  },

  getPullRequests: async (owner: string, repo: string, token?: string): Promise<PullRequest[]> => {
    const api = await getOctokit(token);

    try {
      const allPrs: PullRequest[] = [];
      let hasNextPage = true;
      let cursor: string | null = null;
      let loopCount = 0;
      const MAX_LOOPS = 20; // Safety limit for pagination

      while (hasNextPage && loopCount < MAX_LOOPS) {
        loopCount++;
        const response: any = await api.request('POST /graphql', {
          query: `
            query ($owner: String!, $repo: String!, $cursor: String) {
              repository(owner: $owner, name: $repo) {
                pullRequests(first: 100, states: OPEN, orderBy: {field: UPDATED_AT, direction: DESC}, after: $cursor) {
                  pageInfo {
                    hasNextPage
                    endCursor
                  }
                  nodes {
                    databaseId
                    number
                    title
                    headRefName
                    baseRefName
                    author {
                      login
                      avatarUrl
                    }
                    isDraft
                    mergeable
                    changedFiles
                  }
                }
              }
            }
          `,
          variables: {
            owner,
            repo,
            cursor
          }
        });

        if (response.data.errors) {
          console.error("GraphQL Errors while fetching PRs:", response.data.errors);
          if (!response.data.data) break;
        }

        const data = response.data.data?.repository?.pullRequests;
        if (!data) break;

        const mapped = data.nodes.map((pr: any) => ({
          id: pr.databaseId,
          number: pr.number,
          title: pr.title,
          branch: pr.headRefName,
          targetBranch: pr.baseRefName,
          author: {
            name: pr.author?.login || 'Unknown',
            avatar: pr.author?.avatarUrl || ''
          },
          hasConflicts: pr.mergeable === 'CONFLICTING',
          isDraft: pr.isDraft,
          status: pr.isDraft ? 'draft' : 'open',
          filesChanged: pr.changedFiles
        }));

        allPrs.push(...mapped);

        hasNextPage = data.pageInfo.hasNextPage;
        cursor = data.pageInfo.endCursor;
      }

      return allPrs;
    } catch (e) {
      console.error("Failed to fetch PRs", e);
      throw e;
    }
  },

  getIssue: async (owner: string, repo: string, issueNumber: number): Promise<Issue> => {
    const api = await getOctokit();
    const { data } = await api.issues.get({
      owner,
      repo,
      issue_number: issueNumber
    });

    const issue: Issue = {
      id: data.id,
      number: data.number,
      title: data.title,
      description: data.body || '',
      state: data.state as 'open' | 'closed',
      priority: mapPriority(data.labels),
      labels: (data.labels || []).map((l: any) => l.name),
      type: ((data.labels || []).find((l: any) => l.name === 'bug' || l.name === 'feature' || l.name === 'ui') as any)?.name || 'bug',
      createdAt: data.created_at,
      updatedAt: data.updated_at,
      commentsCount: data.comments,
      reporter: {
        name: data.user?.login || '',
        avatar: data.user?.avatar_url || ''
      },
      comments: []
    };

    if (data.comments > 0) {
      issue.comments = await getIssueComments(owner, repo, issue.number, data.updated_at);
    }

    return issue;
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

  getRepositoryStats: async (owner: string, repo: string, token?: string): Promise<{ issues: number; prs: number }> => {
    const cacheKey = `stats-${owner}-${repo}`;
    const cached = repoStatsCache.get(cacheKey);
    if (cached && Date.now() - cached.timestamp < STATS_CACHE_TTL) {
      return cached.data;
    }

    const api = await getOctokit(token);

    // Fetch total open items (issues + PRs) via efficient repos.get
    // and PR count via search in parallel.
    // open_issues_count includes both issues and PRs.
    const [repoData, prCount] = await Promise.all([
        api.repos.get({ owner, repo }),
        githubService.getOpenPullRequestCount(owner, repo, token)
    ]);

    const totalOpen = repoData.data.open_issues_count;
    // Ensure we don't return negative issues if search index is slightly lagging
    const issuesCount = Math.max(0, totalOpen - prCount);
    const data = { issues: issuesCount, prs: prCount };

    repoStatsCache.set(cacheKey, { timestamp: Date.now(), data });
    return data;
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
