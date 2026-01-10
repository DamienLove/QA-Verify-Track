import { Octokit } from "@octokit/rest";
import { Issue, PullRequest } from '../types';

let octokit: Octokit | null = null;

export const githubService = {
  initialize: (token: string) => {
    octokit = new Octokit({ auth: token });
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
          priority: i.labels.find((l: any) => l.name === 'critical' || l.name === 'high' || l.name === 'medium' || l.name === 'low')?.name || 'medium',
          labels: i.labels.map((l: any) => l.name),
          type: i.labels.find((l: any) => l.name === 'bug' || l.name === 'feature' || l.name === 'ui')?.name || 'bug',
          createdAt: i.created_at,
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
        hasConflicts: false, // Requires detail fetch to check mergeable_state
        isDraft: pr.draft,
        status: pr.draft ? 'draft' : 'open',
        filesChanged: 0 // Requires detail fetch
      }));
    } catch (e) {
      console.error("Failed to fetch PRs", e);
      return [];
    }
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
      // Mapping for specific octokit fields if needed
      if (updates.isDraft === false) {
         // Octokit uses graphql for converting draft to ready, or specific headers. 
         // For simplicity in REST, we might just signal readiness via a label or comment if the API is restricted.
         // But standardized 'ready_for_review' is the way.
      }
      return true;
  },

  addReviewer: async (owner: string, repo: string, pullNumber: number, reviewer: string) => {
     if (!octokit) throw new Error("GitHub service not initialized");
     await octokit.pulls.requestReviewers({
         owner,
         repo,
         pull_number: pullNumber,
         reviewers: [reviewer] // In real app, 'me' isn't valid, needs actual username
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

  resolveConflicts: async (owner: string, repo: string, pullNumber: number) => {
      // GitHub API doesn't support resolving conflicts. This is strictly a UI helper link.
      return true;
  }
};
