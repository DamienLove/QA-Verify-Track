export interface AppConfig {
  id: string;
  name: string;
  platform: 'android' | 'ios' | 'web';
  playStoreUrl?: string; // For auto-populating build number
  buildNumber: string;
}

export interface Repository {
  id: string;
  owner: string;
  name: string;
  displayName?: string;
  apiEndpoint?: string;
  githubToken?: string; // Stored securely in Firestore rules usually, but here in user doc
  avatarUrl?: string;
  apps: AppConfig[];
  isConnected: boolean;
  projects?: string[];
  templates?: string[];
  tests?: Test[];
}

export interface Test {
  id: string;
  description: string;
  lastCheckedBuild?: string; // The build number it was last verified on
}

export interface Issue {
  id: number;
  number: number;
  title: string;
  description: string;
  state: 'open' | 'closed';
  priority: 'low' | 'medium' | 'high' | 'critical';
  labels: string[];
  type: 'bug' | 'feature' | 'ui';
  createdAt: string;
  updatedAt: string;
  commentsCount: number;
  reporter: {
    name: string;
    avatar: string;
  };
  comments: Comment[];
}

export interface Comment {
  id: string;
  text: string;
  buildNumber?: string;
}

export interface PullRequest {
  id: number;
  number: number;
  title: string;
  branch: string;
  targetBranch: string;
  author: {
    name: string;
    avatar: string;
  };
  hasConflicts: boolean;
  isDraft: boolean;
  status: 'draft' | 'open' | 'merged' | 'closed';
  filesChanged: number;
  conflictingFiles?: string[];
}
