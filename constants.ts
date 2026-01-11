import { Issue, PullRequest, Repository } from './types';

export const MOCK_REPOS: Repository[] = [
  {
    id: '1',
    owner: 'acme-inc',
    name: 'mobile-ios',
    displayName: 'Consumer iOS',
    isConnected: true,
    projects: ['iOS Release 2.0', 'Maintenance', 'Q4 Goals'],
    templates: ['Bug Report', 'Feature Request', 'UI Polish'],
    apps: [
      { id: 'a1', name: 'Consumer App', platform: 'ios', buildNumber: '104' }
    ]
  },
  {
    id: '2',
    owner: 'acme-inc',
    name: 'mobile-android',
    displayName: 'Android Suite',
    isConnected: true,
    projects: ['Android Overhaul', 'Driver App V2'],
    templates: ['Bug Report', 'Crash Report', 'Performance Issue'],
    apps: [
      { id: 'a2', name: 'Consumer App', platform: 'android', playStoreUrl: 'https://play.google.com/store/apps/details?id=com.acme.app', buildNumber: '892' },
      { id: 'a3', name: 'Driver App', platform: 'android', buildNumber: '892' }
    ]
  }
];

export const MOCK_ISSUES: Issue[] = [
  {
    id: 101,
    number: 402,
    title: "Login crashes on empty password field",
    description: "Steps to reproduce: 1. Launch app. 2. Tap login. 3. Leave password empty and tap 'Submit'. App force closes immediately.",
    state: 'open',
    priority: 'critical',
    type: 'bug',
    labels: ['bug', 'login'],
    createdAt: '2h ago',
    reporter: { name: 'Alex', avatar: 'https://picsum.photos/40/40?random=1' },
    updatedAt: '2h ago',
    commentsCount: 0,
    comments: []
  },
  {
    id: 102,
    number: 405,
    title: "Profile picture update not saving",
    description: "After uploading a new photo, the spinner spins forever and the image never updates on the profile view.",
    state: 'open',
    priority: 'medium',
    type: 'ui',
    labels: ['ui/ux'],
    createdAt: '5h ago',
    reporter: { name: 'Sarah', avatar: 'https://picsum.photos/40/40?random=2' },
    updatedAt: '5h ago',
    commentsCount: 0,
    comments: []
  },
  {
    id: 103,
    number: 398,
    title: "Dark mode toggle implementation",
    description: "Implement the toggle in settings. Needs to persist across sessions.",
    state: 'open',
    priority: 'low',
    type: 'feature',
    labels: ['feature'],
    createdAt: '1d ago',
    reporter: { name: 'Jim', avatar: 'https://picsum.photos/40/40?random=3' },
    updatedAt: '1d ago',
    commentsCount: 0,
    comments: []
  }
];

export const MOCK_PRS: PullRequest[] = [
  {
    id: 201,
    number: 892,
    title: "Update Payment Gateway SDK",
    branch: "feat/payment-v2",
    targetBranch: "main",
    author: { name: "sarah-j", avatar: "https://picsum.photos/40/40?random=2" },
    hasConflicts: true,
    isDraft: false,
    status: "open",
    filesChanged: 12,
    conflictingFiles: ["src/components/Login.tsx", "src/api/client.ts", "package.json"]
  },
  {
    id: 202,
    number: 895,
    title: "Fix crash on logout",
    branch: "fix/logout-crash",
    targetBranch: "main",
    author: { name: "alexdev", avatar: "https://picsum.photos/40/40?random=1" },
    hasConflicts: false,
    isDraft: false,
    status: "open",
    filesChanged: 2
  },
  {
    id: 203,
    number: 899,
    title: "WIP: New Onboarding Flow",
    branch: "feat/onboarding-v2",
    targetBranch: "main",
    author: { name: "mike-des", avatar: "https://picsum.photos/40/40?random=4" },
    hasConflicts: false,
    isDraft: true,
    status: "draft",
    filesChanged: 24
  }
];
