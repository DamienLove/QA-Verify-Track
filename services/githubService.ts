import { Octokit } from "@octokit/rest";
import { Issue, PullRequest } from '../types';

let octokit: Octokit | null = null;
const commentsCache = new Map<string, { timestamp: string; data: any[] }>();

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
    octokit = new Octokit({ auth: token });
  }, 

  getIssueComments: async (owner: string, repo: string, issueNumber: number, lastUpdated?: string) => {
    if (!octokit) throw new Error("GitHub service not initialized. Please configure your token.");

    const cacheKey = `${owner}/${repo}/${issueNumber}`;

    // Check cache if lastUpdated is provided
    if (lastUpdated) {
      const cached = commentsCache.get(cacheKey);
      if (cached && cached.timestamp === lastUpdated) {
        return cached.data;
      }
    }

    const { data } = await octokit.issues.listComments({
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
    if (!octokit) throw new Error("GitHub service not initialized. Please configure your token.");
    
    try {
      const response = await octokit.issues.listForRepo({
        owner,
        repo,
        state,
        sort: 'updated',
        direction: 'desc',
        per_page: 50
      });

      // Filter out PRs as they are returned in issues endpoint too
      return response.data
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

  getPullRequests: async (owner: string, repo: string): Promise<PullRequest[]> => {
    if (!octokit) throw new Error("GitHub service not initialized");

    try {
      const response = await octokit.pulls.list({
        owner,
        repo,
        state: 'open',
        sort: 'updated',
        direction: 'desc'
      });
      
      return response.data.map((pr: any) => ({
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

  // Get full details for a single PR, including mergeable state
  getPullRequest: async (owner: string, repo: string, pullNumber: number) => {
      if (!octokit) throw new Error("GitHub service not initialized");
      const { data } = await octokit.pulls.get({
          owner,
          repo,
          pull_number: pullNumber
      });
      return data;
  },

  createIssue: async (repoId: string, issue: Partial<Issue>, owner: string, repoName: string) => {
    if (!octokit) throw new Error("GitHub service not initialized");
    
    const response = await octokit.issues.create({
      owner: owner,
      repo: repoName,
      title: issue.title || '',
      body: issue.description,
      labels: issue.labels
    });
    
    return response.data;
  },

  addComment: async (owner: string, repo: string, issueNumber: number, body: string) => {
    if (!octokit) throw new Error("GitHub service not initialized");
    await octokit.issues.createComment({
      owner,
      repo,
      issue_number: issueNumber,
      body
    });
    return true;
  },

  updateIssueStatus: async (owner: string, repo: string, issueNumber: number, state: 'open' | 'closed') => {
    if (!octokit) throw new Error("GitHub service not initialized");
    await octokit.issues.update({
      owner,
      repo,
      issue_number: issueNumber,
      state
    });
    return true;
  },
  
  mergePR: async (owner: string, repo: string, pullNumber: number) => {
    if (!octokit) throw new Error("GitHub service not initialized");
    await octokit.pulls.merge({
        owner,
        repo,
        pull_number: pullNumber
    });
    return true;
  },

  denyPR: async (owner: string, repo: string, pullNumber: number) => {
      if (!octokit) throw new Error("GitHub service not initialized");
      await octokit.pulls.update({
          owner,
          repo,
          pull_number: pullNumber,
          state: 'closed'
      });
      return true;
  },

  updatePR: async (owner: string, repo: string, pullNumber: number, updates: any) => {
      if (!octokit) throw new Error("GitHub service not initialized");

      const { isDraft, ...restUpdates } = updates;

      if (isDraft !== undefined) {
          const { data: pr } = await octokit.pulls.get({
              owner,
              repo,
              pull_number: pullNumber
          });

          // Toggle draft status using GraphQL mutations
          const mutation = isDraft
              ? `mutation($id: ID!) { convertPullRequestToDraft(input: {pullRequestId: $id}) { clientMutationId } }`
              : `mutation($id: ID!) { markPullRequestReadyForReview(input: {pullRequestId: $id}) { clientMutationId } }`;

          await octokit.request('POST /graphql', {
              query: mutation,
              variables: {
                  id: pr.node_id
              }
          });
      }

      // Apply other REST updates if any
      if (Object.keys(restUpdates).length > 0) {
          await octokit.pulls.update({
              owner,
              repo,
              pull_number: pullNumber,
              ...restUpdates
          });
      }

      return true;
  },

  addReviewer: async (owner: string, repo: string, pullNumber: number, reviewer: string) => {
     if (!octokit) throw new Error("GitHub service not initialized");
     await octokit.pulls.requestReviewers({
         owner,
         repo,
         pull_number: pullNumber,
         reviewers: [reviewer] 
     });
     return true;
  },

  approvePR: async (owner: string, repo: string, pullNumber: number) => {
     if (!octokit) throw new Error("GitHub service not initialized");
     await octokit.pulls.createReview({
         owner,
         repo,
         pull_number: pullNumber,
         event: 'APPROVE'
     });
     return true;
  },

  // Try to update branch with base to resolve simple behind-head conflicts
  updateBranch: async (owner: string, repo: string, pullNumber: number) => {
      if (!octokit) throw new Error("GitHub service not initialized");
      try {
          await octokit.pulls.updateBranch({
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

  getOpenIssueCount: async (owner: string, repo: string): Promise<number> => {
    if (!octokit) throw new Error("GitHub service not initialized");
    const response = await octokit.search.issuesAndPullRequests({
      q: `repo:${owner}/${repo} is:issue is:open`,
    });
    return response.data.total_count;
  }
};
