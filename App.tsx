import React, { useState, useEffect, useRef } from 'react';
import { HashRouter, Routes, Route, Link, useNavigate, useLocation, useSearchParams, Navigate, useParams } from 'react-router-dom';
import { Repository, AppConfig, Issue, PullRequest, Comment, GlobalSettings, PullRequestFile, Test } from './types';
import { githubService } from './services/githubService';
import { auth, firebaseService } from './services/firebase';
import { aiService } from './services/aiService';
import { themeService, themes } from './services/themeService';
import { isValidUrl } from './services/security';
import { onAuthStateChanged, signInWithEmailAndPassword, createUserWithEmailAndPassword, signOut, User } from 'firebase/auth';
import Notes from './components/Notes';
import { IssueCard } from './components/IssueCard';

const normalizeCheckedBuilds = (builds?: string[]) => {
  const normalized = (builds || [])
    .map((value) => (value ?? '').toString().trim())
    .filter(Boolean);
  const unique = Array.from(new Set(normalized));
  unique.sort((a, b) => {
    const aNum = parseInt(a, 10);
    const bNum = parseInt(b, 10);
    if (Number.isNaN(aNum) && Number.isNaN(bNum)) return a.localeCompare(b);
    if (Number.isNaN(aNum)) return 1;
    if (Number.isNaN(bNum)) return -1;
    return aNum - bNum;
  });
  return unique;
};

const normalizeTest = (test: Test): Test => {
  const checkedBuilds = normalizeCheckedBuilds(
    test.checkedBuilds ?? (test.lastCheckedBuild ? [test.lastCheckedBuild] : [])
  );
  const lastCheckedBuild = checkedBuilds.length ? checkedBuilds[checkedBuilds.length - 1] : undefined;
  return { ...test, checkedBuilds, lastCheckedBuild };
};

const normalizeTests = (tests?: Test[]) => (tests ?? []).map(normalizeTest);

const getCheckedBuilds = (test: Test) =>
  normalizeCheckedBuilds(test.checkedBuilds ?? (test.lastCheckedBuild ? [test.lastCheckedBuild] : []));

const getLatestCheckedBuild = (test: Test) => {
  const builds = getCheckedBuilds(test);
  return builds.length ? builds[builds.length - 1] : undefined;
};

const normalizeRepos = (repos: Repository[]): Repository[] =>
  repos.map((repo) => ({
    ...repo,
    apps: (repo.apps || []).map((app, index) => ({
      ...app,
      id: app.id || `${repo.id || repo.name || 'repo'}-app-${index}`,
      platform: app.platform || 'android',
      buildNumber: (app.buildNumber ?? '1').toString().trim() || '1',
    })),
    tests: normalizeTests(repo.tests),
  }));

const parseBuildNumber = (value?: string | null) => {
  if (!value) return null;
  const match = value.match(/(\d+)/);
  if (!match) return null;
  const parsed = parseInt(match[1], 10);
  return Number.isNaN(parsed) ? null : parsed;
};

const resolveGithubToken = (repo: Repository, globalSettings?: GlobalSettings) => {
  if (repo.useCustomToken === false) {
    return globalSettings?.globalGithubToken || repo.githubToken || '';
  }
  return repo.githubToken || globalSettings?.globalGithubToken || '';
};

const describeGithubError = (error: unknown) => {
  const status = (error as any)?.status;
  if (status === 401) return 'GitHub token invalid or expired.';
  if (status === 403) return 'GitHub token lacks repo/issues access or is rate-limited.';
  if (status === 404) return 'Repository not found or access denied.';
  return 'Failed to reach GitHub. Check token and network.';
};

// --- Shared UI Components ---

const BottomNav = ({ onNotesClick }: { onNotesClick: () => void }) => {
  const location = useLocation();
  const isConfig = location.pathname.startsWith('/config');
  const isProjects = !isConfig && (
      location.pathname === '/' ||
      location.pathname.startsWith('/dashboard') ||
      location.pathname.startsWith('/issue')
  );

  const getLinkClass = (isActive: boolean) => `flex flex-col items-center justify-center w-16 h-full gap-1 transition-all active:scale-95 focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-2 focus-visible:ring-offset-background-light dark:focus-visible:ring-offset-background-dark rounded-lg outline-none ${isActive ? 'text-primary' : 'text-gray-500 dark:text-gray-400 hover:text-gray-900 dark:hover:text-white'}`;

  return (
    <nav className="fixed bottom-0 left-0 w-full bg-background-light/80 dark:bg-background-dark/90 backdrop-blur-lg border-t border-gray-200 dark:border-white/10 z-50 pb-safe">
      <div className="flex items-center justify-around h-16 max-w-md mx-auto">
          <Link to="/" className={getLinkClass(isProjects)} aria-current={isProjects ? 'page' : undefined}>
              <span className="material-symbols-outlined text-[24px]" aria-hidden="true">grid_view</span>
              <span className="text-[10px] font-bold">Projects</span>
          </Link>
          <button
              onClick={onNotesClick}
              className="flex flex-col items-center justify-center w-16 h-full gap-1 text-gray-500 dark:text-gray-400 hover:text-gray-900 dark:hover:text-white transition-all active:scale-95 focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-2 focus-visible:ring-offset-background-light dark:focus-visible:ring-offset-background-dark rounded-lg outline-none"
              aria-label="Open Notes"
          >
              <span className="material-symbols-outlined text-[24px]" aria-hidden="true">description</span>
              <span className="text-[10px] font-medium">Notes</span>
          </button>
          <Link to="/config" className={getLinkClass(isConfig)} aria-current={isConfig ? 'page' : undefined}>
              <span className="material-symbols-outlined text-[24px]" aria-hidden="true">settings</span>
              <span className="text-[10px] font-medium">Config</span>
          </Link>
      </div>
    </nav>
  );
};

// --- Auth Page ---
const LoginPage = () => {
    const [isLogin, setIsLogin] = useState(true);
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);

    const handleAuth = async (e: React.FormEvent) => {
        e.preventDefault();
        setError('');
        setLoading(true);
        try {
            if (isLogin) {
                await signInWithEmailAndPassword(auth, email, password);
            } else {
                await createUserWithEmailAndPassword(auth, email, password);
            }
        } catch (err: any) {
            setError(err.message.replace('Firebase: ', ''));
        } finally {
            setLoading(false);
        }
    };

    const handleGoogle = async () => {
        try {
            await firebaseService.signInWithGoogle();
        } catch (err: any) {
            setError(err.message);
        }
    };

    return (
        <div className="min-h-screen bg-background-dark flex items-center justify-center p-6">
            <div className="w-full max-w-sm space-y-8 animate-fade-in">
                <div className="text-center space-y-2">
                    <div className="inline-flex items-center justify-center size-16 rounded-2xl bg-primary/20 text-primary mb-4 shadow-[0_0_15px_var(--color-primary)]">
                        <span className="material-symbols-outlined text-4xl">monitor_heart</span>
                    </div>
                    <h1 className="text-3xl font-bold text-white tracking-tight">QA Verify & Track</h1>
                    <p className="text-gray-400">Sync your testing workflow across all devices.</p>
                </div>

                <div className="bg-surface-dark p-6 rounded-2xl border border-white/5 space-y-6">
                    {error && <div className="p-3 bg-red-500/10 border border-red-500/20 text-red-400 text-sm rounded-lg">{error}</div>}
                    
                    <form onSubmit={handleAuth} className="space-y-4">
                        <div className="space-y-1">
                            <label htmlFor="email" className="text-xs uppercase font-bold text-gray-500">Email</label>
                            <input id="email" value={email} onChange={e=>setEmail(e.target.value)} type="email" required maxLength={254} className="w-full bg-input-dark border-transparent rounded-lg p-3 text-white focus:ring-primary focus:border-primary" placeholder="qa@acme.inc" />
                        </div>
                        <div className="space-y-1">
                            <label htmlFor="password" className="text-xs uppercase font-bold text-gray-500">Password</label>
                            <input id="password" value={password} onChange={e=>setPassword(e.target.value)} type="password" required maxLength={128} className="w-full bg-input-dark border-transparent rounded-lg p-3 text-white focus:ring-primary focus:border-primary" placeholder="••••••••" />
                        </div>
                        <button disabled={loading} className="w-full bg-primary text-black font-bold h-12 rounded-lg hover:bg-primary-hover transition-colors disabled:opacity-50">
                            {loading ? 'Processing...' : (isLogin ? 'Sign In' : 'Create Account')}
                        </button>
                    </form>

                    <div className="relative">
                        <div className="absolute inset-0 flex items-center"><div className="w-full border-t border-white/10"></div></div>
                        <div className="relative flex justify-center text-xs uppercase"><span className="bg-surface-dark px-2 text-gray-500">Or continue with</span></div>
                    </div>

                    <button onClick={handleGoogle} className="w-full bg-white text-black font-bold h-12 rounded-lg hover:bg-gray-100 transition-colors flex items-center justify-center gap-2">
                         <img src="https://www.gstatic.com/firebasejs/ui/2.0.0/images/auth/google.svg" className="w-5 h-5" alt="Google" />
                         Google
                    </button>

                    <div className="text-center">
                        <button onClick={() => setIsLogin(!isLogin)} className="text-primary text-sm hover:underline">
                            {isLogin ? "Need an account? Sign up" : "Already have an account? Sign in"}
                        </button>
                    </div>
                </div>

            </div>
        </div>
    );
};

// --- Protected Pages ---

// 1. Home Page: Project Selection
const HomePage = ({
    repos,
    user,
    globalSettings,
    onNotesClick
}: {
    repos: Repository[],
    user: User,
    globalSettings: GlobalSettings,
    onNotesClick: () => void
}) => {
    const [repoStats, setRepoStats] = useState<Record<string, { issues: number; prs: number }>>({});
    const [repoStatsError, setRepoStatsError] = useState<Record<string, string>>({});

    useEffect(() => {
        let isMounted = true;
        const fetchStats = async () => {
            const stats: Record<string, { issues: number; prs: number }> = {};
            const errors: Record<string, string> = {};

            await Promise.all(repos.map(async (repo) => {
                const token = resolveGithubToken(repo, globalSettings);
                if (!token) {
                    errors[repo.id] = 'Missing GitHub token.';
                    return;
                }

                try {
                    // Use parallel fetching with explicit token to avoid singleton race conditions
                    // Also use optimized count-only fetch for PRs instead of fetching full list
                    const [issues, prCount] = await Promise.all([
                        githubService.getOpenIssueCount(repo.owner, repo.name, token),
                        githubService.getOpenPullRequestCount(repo.owner, repo.name, token)
                    ]);
                    stats[repo.id] = { issues, prs: prCount };
                } catch (error) {
                    console.error(`Failed to fetch stats for ${repo.owner}/${repo.name}`, error);
                    errors[repo.id] = describeGithubError(error);
                }
            }));

            if (isMounted) {
                setRepoStats(stats);
                setRepoStatsError(errors);
            }
        };

        if (repos.length > 0) {
            fetchStats();
        } else {
            setRepoStats({});
            setRepoStatsError({});
        }

        return () => {
            isMounted = false;
        };
    }, [repos, globalSettings]);

    return (
        <div className="relative flex h-full min-h-screen w-full flex-col overflow-hidden pb-20">
            <header className="sticky top-0 z-10 flex items-center justify-between bg-background-light/95 dark:bg-background-dark/95 backdrop-blur-md p-4 pb-2 border-b border-gray-200 dark:border-white/5">
                <div className="flex items-center gap-3">
                    <button className="relative group cursor-pointer rounded-full focus:outline-none focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-2 focus-visible:ring-offset-background-light dark:focus-visible:ring-offset-background-dark" onClick={() => signOut(auth)} aria-label="Sign out" title="Sign out">
                        <img src={user.photoURL || `https://ui-avatars.com/api/?name=${user.email}`} className="size-10 rounded-full border-2 border-transparent hover:border-primary transition-colors bg-surface-dark" alt="Profile" />
                        <div className="absolute bottom-0 right-0 size-3 bg-primary rounded-full border-2 border-background-light dark:border-background-dark"></div>
                    </button>
                    <div>
                        <h2 className="text-xl font-bold leading-tight tracking-tight">My Projects</h2>
                        <p className="text-xs text-gray-500 dark:text-gray-400 font-medium truncate max-w-[150px]">{user.email}</p>
                    </div>
                </div>
                <Link to="/config" aria-label="Add new repository" className="flex items-center justify-center size-10 rounded-full bg-white/5 hover:bg-white/10 text-primary border border-primary/20 transition-all">
                    <span className="material-symbols-outlined font-bold">add_link</span>
                </Link>
            </header>

            <main className="flex-1 flex flex-col gap-5 p-4">
                {repos.length === 0 ? (
                    <div className="text-center py-20 opacity-60">
                         <span className="material-symbols-outlined text-6xl mb-4">move_to_inbox</span>
                         <p className="text-lg font-bold">No Projects Found</p>
                         <p className="text-sm">Go to Config to add your first repo.</p>
                    </div>
                ) : (
                    repos.map(repo => (
                        <div key={repo.id} className="group flex flex-col gap-0 rounded-xl glass-card shadow-md hover:shadow-lg transition-shadow overflow-hidden">
                            <div className="p-3 flex items-start justify-between">
                                <div className="flex flex-col gap-0.5">
                                    <div className="flex items-center gap-2 text-gray-400 mb-1">
                                        <span className="material-symbols-outlined text-sm">folder_open</span>
                                        <span className="text-xs font-medium uppercase tracking-wider">{repo.apps[0]?.platform || 'Generic'}</span>
                                    </div>
                                    <h3 className="text-lg font-bold leading-tight group-hover:text-primary transition-colors">
                                        {repo.displayName || repo.name}
                                        <span className="text-xs font-normal text-gray-500 ml-2">({repo.owner}/{repo.name})</span>
                                    </h3>
                                    <div className="flex items-center gap-2 mt-0.5">
                                        <span className="relative flex h-2 w-2">
                                            <span className="relative inline-flex rounded-full h-2 w-2 bg-primary"></span>
                                        </span>
                                        <p className="text-primary text-xs font-bold">Build #{repo.apps[0]?.buildNumber}</p>
                                    </div>
                                    <div className="flex items-center gap-3 text-[11px] text-gray-500 dark:text-gray-400 mt-1">
                                        <span>Issues: <span className="font-semibold text-slate-900 dark:text-white">{repoStats[repo.id]?.issues ?? '—'}</span></span>
                                        <span>PRs: <span className="font-semibold text-slate-900 dark:text-white">{repoStats[repo.id]?.prs ?? '—'}</span></span>
                                    </div>
                                    {repoStatsError[repo.id] && (
                                        <p className="mt-1 text-[10px] text-red-500 dark:text-red-400">
                                            {repoStatsError[repo.id]}
                                        </p>
                                    )}
                                </div>
                                <Link to={`/dashboard?repo=${repo.id}`} aria-label="View repository dashboard" className="flex items-center justify-center size-10 rounded-full bg-gray-100 dark:bg-surface-dark-lighter backdrop-blur-sm text-primary">
                                    <span className="material-symbols-outlined">arrow_forward</span>
                                </Link>
                            </div>
                        </div>
                    ))
                )}
            </main>
            <BottomNav onNotesClick={onNotesClick} />
        </div>
    );
};

// 2. Configuration Page
const ConfigurationPage = ({
    repos,
    setRepos,
    user,
    globalSettings,
    setGlobalSettings,
    onNotesClick
}: {
    repos: Repository[],
    setRepos: React.Dispatch<React.SetStateAction<Repository[]>>,
    user: User,
    globalSettings: GlobalSettings,
    setGlobalSettings: React.Dispatch<React.SetStateAction<GlobalSettings>>,
    onNotesClick: () => void
}) => {
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();
    const repoParam = searchParams.get('repo');
    const [view, setView] = useState<'list' | 'edit' | 'global'>('list');
    const [activeRepoId, setActiveRepoId] = useState<string | null>(null);
    const [saving, setSaving] = useState(false);
    const [saveError, setSaveError] = useState('');

    const [globalForm, setGlobalForm] = useState<GlobalSettings>(globalSettings || {});

    // Form State
    const [formData, setFormData] = useState<Partial<Repository>>({
        name: '',
        owner: '',
        displayName: '',
        githubToken: '',
        apps: []
    });

    // Theme state
    const [currentTheme, setCurrentTheme] = useState(themeService.getSavedThemeId());
    const [isDark, setIsDark] = useState(themeService.getSavedMode());

    const changeTheme = (themeId: string) => {
        themeService.applyTheme(themeId);
        setCurrentTheme(themeId);
    };

    const toggleMode = () => {
        const newMode = !isDark;
        themeService.setDarkMode(newMode);
        setIsDark(newMode);
    };

    useEffect(() => {
        setGlobalForm(globalSettings || {});
    }, [globalSettings]);

    const useCustomToken = formData.useCustomToken !== false;

    const startEdit = (repo?: Repository) => {
        if (repo) {
            setActiveRepoId(repo.id);
            setFormData({ ...repo, useCustomToken: repo.useCustomToken !== false });
        } else {
            setActiveRepoId(null);
            setFormData({
                id: Date.now().toString(),
                name: '',
                owner: '',
                displayName: '',
                githubToken: '',
                useCustomToken: true,
                apps: [{ id: Date.now().toString(), name: 'New App', platform: 'android', buildNumber: '1' }],
                projects: [],
                templates: []
            });
        }
        setView('edit');
    };

    useEffect(() => {
        if (!repoParam || repos.length === 0) return;
        if (activeRepoId === repoParam && view === 'edit') return;
        const repo = repos.find(r => r.id === repoParam);
        if (repo) {
            startEdit(repo);
        }
    }, [repoParam, repos, activeRepoId, view]);

    const addApp = () => {
        const newApp: AppConfig = {
             id: Date.now().toString(),
             name: '',
             platform: 'android',
             buildNumber: '1'
        };
        setFormData(prev => ({ ...prev, apps: [...(prev.apps || []), newApp] }));
    };

    const updateApp = (index: number, field: keyof AppConfig, value: string) => {
        const newApps = [...(formData.apps || [])];
        newApps[index] = { ...newApps[index], [field]: value };
        setFormData(prev => ({ ...prev, apps: newApps }));
    };

    const removeApp = (index: number) => {
        const newApps = [...(formData.apps || [])];
        newApps.splice(index, 1);
        setFormData(prev => ({ ...prev, apps: newApps }));
    }

    const saveRepo = async () => {
        if (!formData.name || !formData.owner) {
            setSaveError('Owner and repo name are required.');
            return;
        }

        // Validate Owner (GitHub username format: alphanumeric, single hyphens, no start/end hyphen)
        const ownerRegex = /^[a-z\d](?:[a-z\d]|-(?=[a-z\d])){0,38}$/i;
        if (!ownerRegex.test(formData.owner)) {
            setSaveError('Invalid owner name. Use only alphanumeric characters and single hyphens.');
            return;
        }

        // Validate Repo Name (alphanumeric, periods, underscores, hyphens)
        const repoRegex = /^[\w.-]+$/;
        if (!repoRegex.test(formData.name)) {
            setSaveError('Invalid repository name. Use only alphanumeric characters, underscores, hyphens, and periods.');
            return;
        }

        // Validate Token (no whitespace)
        if (formData.useCustomToken !== false && formData.githubToken) {
            if (/\s/.test(formData.githubToken)) {
                setSaveError('Token must not contain whitespace.');
                return;
            }
        }

        // Validate App URLs (Security: Prevent XSS in Dashboard)
        if (formData.apps) {
            for (const app of formData.apps) {
                if (app.playStoreUrl && !isValidUrl(app.playStoreUrl)) {
                    setSaveError(`Invalid URL for app "${app.name || 'Unknown'}". Allowed protocols: http, https, market, itms-apps, itms-services.`);
                    return;
                }
            }
        }

        setSaveError('');
        setSaving(true);

        try {
            let updatedRepos;
            if (activeRepoId) {
                updatedRepos = repos.map(r => r.id === activeRepoId ? { ...r, ...formData } as Repository : r);
            } else {
                updatedRepos = [...repos, { ...formData, isConnected: true } as Repository];
            }

            // Optimistically update local state for immediate UI feedback
            setRepos(updatedRepos);

            // Persist to Firestore
            await firebaseService.saveUserRepos(user.uid, updatedRepos);
            setView('list');
        } catch (error: any) {
            console.error('Failed to save repository', error);
            setSaveError(error?.message || 'Failed to save. Check your network or Firebase rules.');
        } finally {
            setSaving(false);
        }
    };

    const deleteRepo = async () => {
        if (!activeRepoId) return;
        setSaveError('');
        setSaving(true);
        try {
            const updatedRepos = repos.filter(r => r.id !== activeRepoId);
            setRepos(updatedRepos); // optimistic
            await firebaseService.saveUserRepos(user.uid, updatedRepos);
            setView('list');
        } catch (error: any) {
            console.error('Failed to delete repository', error);
            setSaveError(error?.message || 'Failed to delete. Check your network or Firebase rules.');
        } finally {
            setSaving(false);
        }
    };

    const startGlobalEdit = () => {
        setSaveError('');
        setSaving(false);
        setGlobalForm(globalSettings || {});
        setView('global');
    };

    const saveGlobalSettings = async () => {
        setSaveError('');
        setSaving(true);
        try {
            const nextSettings = { ...globalForm };
            setGlobalSettings(nextSettings);
            await firebaseService.saveGlobalSettings(user.uid, nextSettings);
            setView('list');
        } catch (error: any) {
            console.error('Failed to save global settings', error);
            setSaveError(error?.message || 'Failed to save global settings.');
        } finally {
            setSaving(false);
        }
    };

    if (view === 'list') {
        return (
            <div className="bg-background-light dark:bg-background-dark min-h-screen pb-24">
                <header className="sticky top-0 z-20 flex items-center justify-between p-4 bg-background-light/95 dark:bg-background-dark/95 backdrop-blur-md border-b border-gray-200 dark:border-white/5">
                    <div className="flex items-center gap-3">
                        <button onClick={() => navigate(-1)} aria-label="Go back" className="p-1 rounded-full hover:bg-black/5 dark:hover:bg-white/10">
                            <span className="material-symbols-outlined text-slate-900 dark:text-white">arrow_back</span>
                        </button>
                        <h1 className="text-lg font-bold">Configuration</h1>
                    </div>
                </header>
                <main className="p-4 space-y-6">
                    <section className="space-y-4">
                        <div className="flex justify-between items-center">
                            <h2 className="text-xs uppercase text-gray-500 font-bold tracking-wider">Appearance</h2>
                        </div>
                        <div className="p-4 bg-white dark:bg-surface-dark rounded-xl border border-gray-200 dark:border-white/5 space-y-4">
                            <div className="flex items-center justify-between">
                                <label htmlFor="dark-mode-toggle" className="text-sm font-medium text-slate-900 dark:text-white cursor-pointer">Dark Mode</label>
                                <button
                                    id="dark-mode-toggle"
                                    role="switch"
                                    aria-checked={isDark}
                                    aria-label="Dark Mode"
                                    onClick={toggleMode}
                                    className={`w-12 h-6 rounded-full p-1 transition-colors ${isDark ? 'bg-primary' : 'bg-gray-300'}`}
                                >
                                    <div className={`w-4 h-4 bg-white rounded-full transition-transform ${isDark ? 'translate-x-6' : ''}`}></div>
                                </button>
                            </div>
                            <div className="space-y-2 qavt-theme-picker">
                                <label className="text-sm font-medium text-slate-900 dark:text-white">Theme Color</label>
                                <div className="grid grid-cols-5 gap-3 qavt-theme-grid">
                                    {themes.map(t => (
                                        <button
                                            key={t.id}
                                            onClick={() => changeTheme(t.id)}
                                            className={`qavt-theme-swatch w-full aspect-square rounded-lg border-2 ${currentTheme === t.id ? 'border-slate-900 dark:border-white scale-110' : 'border-transparent'} transition-all shadow-sm`}
                                            style={{ backgroundColor: t.colors.primary }}
                                            title={t.name}
                                        />
                                    ))}
                                </div>
                                <p className="text-center text-xs text-gray-500 mt-1">{themes.find(t => t.id === currentTheme)?.name}</p>
                            </div>
                        </div>
                    </section>

                    <section className="space-y-4">
                        <div className="flex justify-between items-center">
                            <h2 className="text-xs uppercase text-gray-500 font-bold tracking-wider">Global GitHub</h2>
                            <button onClick={startGlobalEdit} className="text-xs flex items-center gap-1 bg-black/5 dark:bg-white/5 px-2 py-1 rounded text-slate-900 dark:text-white hover:bg-black/10 dark:hover:bg-white/10">
                                <span className="material-symbols-outlined text-sm">settings</span>
                                Edit
                            </button>
                        </div>
                        <div className="p-4 bg-white dark:bg-surface-dark rounded-xl border border-gray-200 dark:border-white/5 space-y-2">
                            <div className="text-sm font-medium text-slate-900 dark:text-white">GitHub PAT</div>
                            <p className="text-xs text-gray-500">
                                {globalSettings?.globalGithubToken ? 'Set (used when repo token is disabled)' : 'Not set'}
                            </p>
                        </div>
                    </section>

                    <section className="space-y-4">
                        <div className="flex justify-between items-center">
                            <h2 className="text-xs uppercase text-gray-500 font-bold tracking-wider">Repositories</h2>
                            <button onClick={() => startEdit()} className="flex items-center gap-1 bg-primary text-black px-3 py-1.5 rounded-lg text-sm font-bold">
                                <span className="material-symbols-outlined text-lg">add</span> New
                            </button>
                        </div>
                        {repos.map(repo => (
                            <div key={repo.id} onClick={() => startEdit(repo)} className="p-4 rounded-xl glass-card flex justify-between items-center cursor-pointer active:scale-[0.99] transition-transform">
                                <div>
                                    <h3 className="font-bold text-slate-900 dark:text-white">{repo.displayName || repo.name}</h3>
                                    <p className="text-xs text-gray-500">{repo.owner}/{repo.name}</p>
                                </div>
                                <span className="material-symbols-outlined text-gray-400">edit</span>
                            </div>
                        ))}
                    </section>
                </main>
                <BottomNav onNotesClick={onNotesClick} />
            </div>
        );
    }

    if (view === 'global') {
        return (
            <div className="bg-background-light dark:bg-background-dark min-h-screen pb-safe">
                <header className="sticky top-0 z-20 flex items-center justify-between p-4 bg-background-light/95 dark:bg-background-dark/95 backdrop-blur-md border-b border-gray-200 dark:border-white/5">
                    <div className="flex items-center gap-3">
                        <button onClick={() => setView('list')} aria-label="Close global settings" className="p-1 rounded-full hover:bg-black/5 dark:hover:bg-white/10">
                            <span className="material-symbols-outlined text-slate-900 dark:text-white">close</span>
                        </button>
                        <h1 className="text-lg font-bold">Global Settings</h1>
                    </div>
                    <button
                        onClick={saveGlobalSettings}
                        disabled={saving}
                        className="text-primary font-bold text-sm disabled:opacity-50 disabled:pointer-events-none flex items-center gap-2"
                    >
                        {saving && <span className="size-4 border-2 border-current border-t-transparent rounded-full animate-spin"></span>}
                        {saving ? 'Saving...' : 'Save'}
                    </button>
                </header>
                {saveError && (
                    <div className="mx-4 mt-3 p-3 bg-red-500/10 border border-red-500/30 text-red-400 text-sm rounded-lg">
                        {saveError}
                    </div>
                )}
                <main className="p-4 space-y-6">
                    <section className="space-y-4">
                        <h2 className="text-sm font-bold uppercase tracking-wider text-primary">GitHub</h2>
                        <div className="space-y-2">
                            <label htmlFor="global-token" className="text-xs text-gray-500">Global GitHub Personal Access Token (PAT)</label>
                            <input
                                id="global-token"
                                value={globalForm.globalGithubToken || ''}
                                onChange={(e) => setGlobalForm({ ...globalForm, globalGithubToken: e.target.value })}
                                type="password"
                                placeholder="ghp_..."
                                maxLength={512}
                                className="w-full bg-white dark:bg-input-dark border-gray-200 dark:border-white/10 rounded-lg px-4 py-3 text-sm focus:ring-primary focus:border-primary text-slate-900 dark:text-white"
                            />
                            <p className="text-[10px] text-gray-500">Used for repositories that disable per-repo tokens.</p>
                        </div>
                    </section>
                </main>
            </div>
        );
    }

    return (
        <div className="bg-background-light dark:bg-background-dark min-h-screen pb-safe">
            <header className="sticky top-0 z-20 flex items-center justify-between p-4 bg-background-light/95 dark:bg-background-dark/95 backdrop-blur-md border-b border-gray-200 dark:border-white/5">
                <div className="flex items-center gap-3">
                    <button onClick={() => setView('list')} aria-label="Close configuration" className="p-1 rounded-full hover:bg-black/5 dark:hover:bg-white/10">
                        <span className="material-symbols-outlined text-slate-900 dark:text-white">close</span>
                    </button>
                    <h1 className="text-lg font-bold">{activeRepoId ? 'Edit Repository' : 'Add Repository'}</h1>
                </div>
                <div className="flex items-center gap-2">
                    {activeRepoId && (
                        <button
                            onClick={deleteRepo}
                            disabled={saving}
                            className="text-red-500 font-bold text-sm disabled:opacity-50 disabled:pointer-events-none flex items-center gap-1 border border-red-500/40 rounded-md px-2 py-1"
                        >
                            <span className="material-symbols-outlined text-base">delete</span>
                            Delete
                        </button>
                    )}
                    <button
                        onClick={saveRepo}
                        disabled={saving}
                        className="text-primary font-bold text-sm disabled:opacity-50 disabled:pointer-events-none flex items-center gap-2"
                    >
                        {saving && <span className="size-4 border-2 border-current border-t-transparent rounded-full animate-spin"></span>}
                        {saving ? 'Saving...' : 'Save'}
                    </button>
                </div>
            </header>

            {saveError && (
                <div className="mx-4 mt-3 p-3 bg-red-500/10 border border-red-500/30 text-red-400 text-sm rounded-lg">
                    {saveError}
                </div>
            )}

            <main className="p-4 space-y-6">
                <section className="space-y-4">
                    <h2 className="text-sm font-bold uppercase tracking-wider text-primary">GitHub Details</h2>
                    <div className="space-y-3">
                        <div className="space-y-1">
                            <label htmlFor="repo-display-name" className="text-xs text-gray-500">Display Name</label>
                            {/* SECURITY: Enforce length limit to prevent potential DoS */}
                            <input id="repo-display-name" value={formData.displayName} onChange={e => setFormData({...formData, displayName: e.target.value})} type="text" maxLength={100} className="w-full bg-white dark:bg-input-dark border-gray-200 dark:border-white/10 rounded-lg px-4 py-3 text-sm focus:ring-primary focus:border-primary text-slate-900 dark:text-white" autoFocus />
                        </div>
                        <div className="grid grid-cols-2 gap-3">
                            <div className="space-y-1">
                                <label htmlFor="repo-owner" className="text-xs text-gray-500">Owner</label>
                                {/* SECURITY: Enforce length limit (GitHub username max 39 chars) */}
                                <input id="repo-owner" value={formData.owner} onChange={e => setFormData({...formData, owner: e.target.value})} type="text" maxLength={39} placeholder="org" className="w-full bg-white dark:bg-input-dark border-gray-200 dark:border-white/10 rounded-lg px-4 py-3 text-sm focus:ring-primary focus:border-primary text-slate-900 dark:text-white" />
                            </div>
                            <div className="space-y-1">
                                <label htmlFor="repo-name" className="text-xs text-gray-500">Repo Name</label>
                                {/* SECURITY: Enforce length limit (GitHub repo name max 100 chars) */}
                                <input id="repo-name" value={formData.name} onChange={e => setFormData({...formData, name: e.target.value})} type="text" maxLength={100} placeholder="repo" className="w-full bg-white dark:bg-input-dark border-gray-200 dark:border-white/10 rounded-lg px-4 py-3 text-sm focus:ring-primary focus:border-primary text-slate-900 dark:text-white" />
                            </div>
                        </div>
                        <div className="space-y-2">
                            <div className="flex items-center justify-between">
                                <label htmlFor="repo-token-toggle" className="text-xs text-gray-500 cursor-pointer">Use repo-specific PAT</label>
                                <button
                                    id="repo-token-toggle"
                                    role="switch"
                                    aria-checked={useCustomToken}
                                    aria-label="Use repo-specific PAT"
                                    onClick={() => setFormData({ ...formData, useCustomToken: !useCustomToken })}
                                    className={`w-12 h-6 rounded-full p-1 transition-colors ${useCustomToken ? 'bg-primary' : 'bg-gray-300'}`}
                                    type="button"
                                >
                                    <div className={`w-4 h-4 bg-white rounded-full transition-transform ${useCustomToken ? 'translate-x-6' : ''}`}></div>
                                </button>
                            </div>
                            {useCustomToken ? (
                                <>
                                    <label htmlFor="repo-token" className="text-xs text-gray-500">GitHub Personal Access Token (PAT)</label>
                                    {/* SECURITY: Enforce length limit to prevent large payload injection */}
                                    <input id="repo-token" value={formData.githubToken || ''} onChange={e => setFormData({...formData, githubToken: e.target.value})} type="password" maxLength={512} placeholder="ghp_..." className="w-full bg-white dark:bg-input-dark border-gray-200 dark:border-white/10 rounded-lg px-4 py-3 text-sm focus:ring-primary focus:border-primary text-slate-900 dark:text-white" />
                                    <p className="text-[10px] text-gray-500">Used for reading/writing to this repository.</p>
                                </>
                            ) : (
                                <div className="rounded-lg border border-gray-200 dark:border-white/10 bg-white/60 dark:bg-input-dark px-3 py-2 text-xs text-gray-500">
                                    Using global GitHub token. {globalSettings?.globalGithubToken ? 'Global token is set.' : 'Global token is not set yet.'}
                                </div>
                            )}
                        </div>
                    </div>
                </section>

                <div className="h-px bg-gray-200 dark:bg-white/5"></div>

                <section className="space-y-4">
                    <div className="flex justify-between items-center">
                        <h2 className="text-sm font-bold uppercase tracking-wider text-primary">Apps & Builds</h2>
                        <button onClick={addApp} className="text-xs flex items-center gap-1 bg-black/5 dark:bg-white/5 px-2 py-1 rounded text-slate-900 dark:text-white hover:bg-black/10 dark:hover:bg-white/10">
                            <span className="material-symbols-outlined text-sm">add</span> Add App
                        </button>
                    </div>
                    
                    {formData.apps?.map((app, idx) => (
                        <div key={idx} className="p-4 bg-white dark:bg-surface-dark rounded-xl border border-gray-200 dark:border-white/5 space-y-3 relative group">
                            <button onClick={() => removeApp(idx)} aria-label="Delete app" className="absolute top-2 right-2 p-1 rounded hover:bg-red-500/10 text-gray-400 hover:text-red-500">
                                <span className="material-symbols-outlined text-lg">delete</span>
                            </button>
                            <div className="grid grid-cols-2 gap-3 pr-6">
                                <div className="space-y-1">
                                    <label htmlFor={`app-name-${idx}`} className="text-[10px] uppercase text-gray-500">App Name</label>
                                    <input id={`app-name-${idx}`} value={app.name} onChange={e => updateApp(idx, 'name', e.target.value)} type="text" maxLength={50} className="w-full bg-background-light dark:bg-background-dark border-transparent rounded px-2 py-1.5 text-sm text-slate-900 dark:text-white" />
                                </div>
                                <div className="space-y-1">
                                    <label htmlFor={`app-platform-${idx}`} className="text-[10px] uppercase text-gray-500">Platform</label>
                                    <select id={`app-platform-${idx}`} value={app.platform} onChange={e => updateApp(idx, 'platform', e.target.value as any)} className="w-full bg-background-light dark:bg-background-dark border-transparent rounded px-2 py-1.5 text-sm text-slate-900 dark:text-white">
                                        <option value="android">Android</option>
                                        <option value="ios">iOS</option>
                                        <option value="web">Web</option>
                                    </select>
                                </div>
                            </div>
                            <div className="space-y-1">
                                <label htmlFor={`app-url-${idx}`} className="text-[10px] uppercase text-gray-500">Test URL (Play Store/TestFlight)</label>
                                <input id={`app-url-${idx}`} value={app.playStoreUrl || ''} onChange={e => updateApp(idx, 'playStoreUrl', e.target.value)} type="text" placeholder="https://..." maxLength={2048} className="w-full bg-background-light dark:bg-background-dark border-transparent rounded px-2 py-1.5 text-sm text-slate-900 dark:text-white" />
                            </div>
                            <div className="space-y-1">
                                <label htmlFor={`app-build-${idx}`} className="text-[10px] uppercase text-gray-500">Current Build #</label>
                                <input id={`app-build-${idx}`} value={app.buildNumber} onChange={e => updateApp(idx, 'buildNumber', e.target.value)} type="text" maxLength={20} className="w-24 bg-background-light dark:bg-background-dark border-transparent rounded px-2 py-1.5 text-sm font-mono font-bold text-slate-900 dark:text-white" />
                            </div>
                        </div>
                    ))}
                </section>
            </main>
        </div>
    );
};

// 3. Dashboard
const Dashboard = ({ repos, user, globalSettings, onNotesClick }: { repos: Repository[], user: User, globalSettings: GlobalSettings, onNotesClick: () => void }) => {
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();
    const repoId = searchParams.get('repo');
    const conflictPrParam = searchParams.get('conflictPr');
    const repo = repos.find(r => r.id === repoId) || repos[0];

    const [tab, setTab] = useState<'issues' | 'prs' | 'tests'>(conflictPrParam ? 'prs' : 'issues');
    const [activeAppId, setActiveAppId] = useState(repo?.apps[0]?.id || '');
    const [buildNumber, setBuildNumber] = useState(repo?.apps[0]?.buildNumber || '0');
    const activeApp = repo?.apps.find(app => app.id === activeAppId) || repo?.apps[0];
    const activeToken = repo ? resolveGithubToken(repo, globalSettings) : '';

    // Performance optimization: Use refs for frequent updates to prevent re-creating callbacks
    // This prevents the entire issue list from re-rendering on every keystroke in the build number input
    const buildNumberRef = useRef(buildNumber);
    const repoRef = useRef(repo);

    useEffect(() => {
        buildNumberRef.current = buildNumber;
        repoRef.current = repo;
    }, [buildNumber, repo]);

    // Test State
    const [tests, setTests] = useState(() => normalizeTests(repo?.tests));
    const [generatingTests, setGeneratingTests] = useState(false);
    const [newTestDesc, setNewTestDesc] = useState('');
    const [pendingConflictPr, setPendingConflictPr] = useState<number | null>(null);

    useEffect(() => {
        if (repo) {
            setTests(normalizeTests(repo.tests));
        }
    }, [repo]);

    useEffect(() => {
        if (!conflictPrParam) return;
        const parsed = parseInt(conflictPrParam, 10);
        if (Number.isNaN(parsed)) return;
        setPendingConflictPr(parsed);
        setTab('prs');
    }, [conflictPrParam]);

    useEffect(() => {
        if (!repo?.apps?.length) {
            setActiveAppId('');
            return;
        }
        if (!repo.apps.some(app => app.id === activeAppId)) {
            setActiveAppId(repo.apps[0].id);
        }
    }, [repo?.id, repo?.apps, activeAppId]);

    useEffect(() => {
        if (activeApp?.buildNumber) {
            setBuildNumber(activeApp.buildNumber);
        }
    }, [activeApp?.id, activeApp?.buildNumber]);

    const persistBuildNumber = React.useCallback(async (value: string, appId?: string) => {
        if (!repo) return;
        const targetId = appId || activeApp?.id;
        if (!targetId) return;
        const updatedApps = repo.apps.map(app =>
            app.id === targetId ? { ...app, buildNumber: value } : app
        );
        const updatedRepo = { ...repo, apps: updatedApps, tests: normalizeTests(tests) };
        const updatedRepos = repos.map(r => (r.id === repo.id ? updatedRepo : r));
        try {
            await firebaseService.saveUserRepos(user.uid, updatedRepos);
        } catch (e) {
            console.error("Failed to save build number", e);
        }
    }, [repo, activeApp?.id, tests, repos, user.uid]);

    // Debounce build number persistence to prevent Firebase write spam
    useEffect(() => {
        const value = (buildNumber || '').trim();
        // Avoid saving if the value matches what we already have (e.g. on load/switch)
        const currentStored = (activeApp?.buildNumber || '').trim();
        if (value === currentStored) return;

        const timer = setTimeout(() => {
            persistBuildNumber(value);
        }, 1000);

        return () => clearTimeout(timer);
    }, [buildNumber, activeApp?.buildNumber, persistBuildNumber]);

    const persistBuildNumberRef = useRef(persistBuildNumber);
    useEffect(() => {
        persistBuildNumberRef.current = persistBuildNumber;
    });

    // Debounce build number persistence
    useEffect(() => {
        const timer = setTimeout(() => {
            const currentAppBuild = activeApp?.buildNumber;
            // Only save if the value has actually changed from what is in the app config
            if (currentAppBuild !== buildNumber) {
                persistBuildNumberRef.current(buildNumber);
            }
        }, 1000);
        return () => clearTimeout(timer);
    }, [buildNumber, activeApp]);

    const handleSaveTests = async (updatedTests: Test[]) => {
        if (!repo) return;
        const normalizedTests = normalizeTests(updatedTests);
        const updatedRepo = { ...repo, tests: normalizedTests };
        const updatedRepos = repos.map(r => r.id === repo.id ? updatedRepo : r);

        // Save to Firebase
        try {
            await firebaseService.saveUserRepos(user.uid, updatedRepos);
        } catch (e) {
            console.error("Failed to save tests", e);
        }
    };

    const handleGenerateTests = async () => {
        setGeneratingTests(true);
        try {
            const generated = await aiService.generateTests(repo.name, repo.displayName || repo.name);
            const newTests = generated.map(t => ({
                id: Date.now().toString() + Math.random().toString().slice(2),
                description: t,
                checkedBuilds: []
            }));
            const updatedTests = normalizeTests([...tests, ...newTests]);
            setTests(updatedTests);
            await handleSaveTests(updatedTests);
        } finally {
            setGeneratingTests(false);
        }
    };

    const handleAddTest = async () => {
        if (!newTestDesc.trim()) return;
        const newTest = {
            id: Date.now().toString(),
            description: newTestDesc,
            checkedBuilds: []
        };
        const updatedTests = normalizeTests([...tests, newTest]);
        setTests(updatedTests);
        setNewTestDesc('');
        await handleSaveTests(updatedTests);
    };

    const openTestsForApp = (app: AppConfig) => {
        setActiveAppId(app.id);
        setTab('tests');
        if (app.buildNumber) {
            setBuildNumber(app.buildNumber);
        }
    };

    const toggleTest = async (testId: string) => {
        const activeBuild = (buildNumber || '').toString().trim();
        if (!activeBuild) return;
        const updatedTests = tests.map(t => {
            if (t.id !== testId) return t;
            const buildSet = new Set(getCheckedBuilds(t));
            if (buildSet.has(activeBuild)) {
                buildSet.delete(activeBuild);
            } else {
                buildSet.add(activeBuild);
            }
            const nextBuilds = normalizeCheckedBuilds(Array.from(buildSet));
            return {
                ...t,
                checkedBuilds: nextBuilds,
                lastCheckedBuild: nextBuilds.length ? nextBuilds[nextBuilds.length - 1] : undefined
            };
        });
        setTests(updatedTests);
        await handleSaveTests(updatedTests);
    };

    // Ensure GitHub client is ready whenever the selected repo/token changes
    useEffect(() => {
        if (activeToken) {
            githubService.initialize(activeToken);
        }
    }, [activeToken]);

    // Real Data State
    const [allIssues, setAllIssues] = useState<Issue[]>([]);
    const [issueBuildMap, setIssueBuildMap] = useState<Record<number, number>>({});
    const [issueVerifyFixMap, setIssueVerifyFixMap] = useState<Record<number, number>>({});
    const [prs, setPrs] = useState<PullRequest[]>([]);
    const [loading, setLoading] = useState(false);
    const [syncType, setSyncType] = useState<'github' | 'store' | null>(null);
    
    // State for PR actions
    const [prProcessing, setPrProcessing] = useState<number | null>(null);
    const [undoPr, setUndoPr] = useState<{id: number, pr: PullRequest} | null>(null);
    const [manualResolveIds, setManualResolveIds] = useState<Set<number>>(new Set());
    const [selectedPrIds, setSelectedPrIds] = useState<Set<number>>(new Set());
    const [bulkProcessing, setBulkProcessing] = useState(false);
    const selectedPrs = prs.filter(pr => selectedPrIds.has(pr.id));
    const allPrsSelected = prs.length > 0 && selectedPrIds.size === prs.length;
    const somePrsSelected = selectedPrIds.size > 0 && !allPrsSelected;
    const selectAllRef = useRef<HTMLInputElement | null>(null);

    // Conflict resolution modal state
    const [conflictPr, setConflictPr] = useState<PullRequest | null>(null);
    const [conflictFiles, setConflictFiles] = useState<PullRequestFile[]>([]);
    const [activeConflictFile, setActiveConflictFile] = useState<PullRequestFile | null>(null);
    const [conflictFileData, setConflictFileData] = useState<{ base: string; head: string; headSha: string } | null>(null);
    const [conflictLoading, setConflictLoading] = useState(false);
    const [conflictError, setConflictError] = useState('');
    const [resolvedFiles, setResolvedFiles] = useState<Set<string>>(new Set());
    const [resolvingFile, setResolvingFile] = useState(false);
    type ConflictSegment =
        | { type: 'equal'; lines: string[] }
        | { type: 'conflict'; baseLines: string[]; headLines: string[]; resolution?: 'current' | 'incoming' | 'both' };
    const [conflictSegments, setConflictSegments] = useState<ConflictSegment[]>([]);
    const [conflictSegmentOrder, setConflictSegmentOrder] = useState<number[]>([]);
    const [conflictCursor, setConflictCursor] = useState(0);
    const [fileConflictCounts, setFileConflictCounts] = useState<Record<string, number>>({});
    const [selectedConflictFiles, setSelectedConflictFiles] = useState<Set<string>>(new Set());
    const [conflictsCleared, setConflictsCleared] = useState(false);

    // AI Analysis State
  const [analyzingIds, setAnalyzingIds] = useState<Set<number>>(new Set());
  const [analysisResults, setAnalysisResults] = useState<Record<number, string>>({});
  const [syncError, setSyncError] = useState<string>('');
  const [prError, setPrError] = useState<string>('');
  const [issueActionIds, setIssueActionIds] = useState<Set<number>>(new Set());
  const [issueActionError, setIssueActionError] = useState('');
  const [blockIssue, setBlockIssue] = useState<Issue | null>(null);
  const [blockReason, setBlockReason] = useState('');
  const [blockSaving, setBlockSaving] = useState(false);
  const [blockError, setBlockError] = useState('');
  const autoFixAvailable = typeof window !== 'undefined' && Boolean((window as any).QAVT_AUTO_FIX?.issueCreated);

  // Use a ref to track analysis state for stable callbacks
  const analyzingIdsRef = React.useRef(new Set<number>());
  useEffect(() => {
      analyzingIdsRef.current = analyzingIds;
  }, [analyzingIds]);

    // Initial Fetch
    useEffect(() => {
        if (repo && activeToken) {
            handleSync(false); // Initial load without auto-populating build number
        }
    }, [repo, activeToken]);

    // fetchStoreBuild: when true, try to auto-populate build number from stored app config (no external store fetch to avoid CORS)
    const handleSync = async (fetchStoreBuild: boolean = false) => {
        setLoading(true);
        setSyncType(fetchStoreBuild ? 'store' : 'github');
        setSyncError('');
        if (!activeToken) {
            setSyncError('Missing GitHub token. Configure a global or repo token.');
            setLoading(false);
            setSyncType(null);
            return;
        }
        if (fetchStoreBuild) {
             const storedBuild = activeApp?.buildNumber;
             if (storedBuild) {
                 setBuildNumber(storedBuild.toString());
             }
        }

        try {
            const [fetchedIssues, fetchedPrs] = await Promise.all([
                githubService.getIssues(repo.owner, repo.name, 'open', true),
                githubService.getPullRequests(repo.owner, repo.name)
            ]);

            const statusRegex = /\b(open|closed|blocked|fixed)\b[^\d]*(?:build\s*)?v?\s*(\d+)\b/gi;
            const verifyFixRegex = /\bverify\s*fix(?:es)?\b[^\d]*v?\s*(\d+)\b/gi;
            const statusBuildMap: Record<number, number> = {};
            const verifyFixBuildMap: Record<number, number> = {};

            fetchedIssues.forEach((issue) => {
                if (issue.commentsCount <= 0 || !issue.comments) {
                    return;
                }
                try {
                  let maxStatusBuild = -1;
                  let maxVerifyBuild = -1;
                  for (const c of issue.comments) {
                    const body = c.text || '';
                    let m;
                    statusRegex.lastIndex = 0;
                    while ((m = statusRegex.exec(body)) !== null) {
                      const b = parseInt(m[2], 10);
                      if (!isNaN(b)) {
                        maxStatusBuild = Math.max(maxStatusBuild, b);
                      }
                    }
                    let v;
                    verifyFixRegex.lastIndex = 0;
                    while ((v = verifyFixRegex.exec(body)) !== null) {
                      const b = parseInt(v[1], 10);
                      if (!isNaN(b)) {
                        maxVerifyBuild = Math.max(maxVerifyBuild, b);
                      }
                    }
                  }
                  if (maxStatusBuild >= 0) {
                    statusBuildMap[issue.id] = maxStatusBuild;
                  }
                  if (maxVerifyBuild >= 0) {
                    verifyFixBuildMap[issue.id] = maxVerifyBuild;
                  }
                } catch (err) {
                  console.error('Failed to parse comments for issue', issue.number, err);
                }
            });

            setAllIssues(fetchedIssues);
            setIssueBuildMap(statusBuildMap);
            setIssueVerifyFixMap(verifyFixBuildMap);
            setPrs(fetchedPrs);
        } catch (error) {
            console.error("Sync failed", error);
            setSyncError(describeGithubError(error));
        } finally {
            setLoading(false);
            setSyncType(null);
        }
    };

    const issues = React.useMemo(() => {
        const targetBuild = parseBuildNumber(buildNumber);
        return allIssues.filter(issue => {
            const verifyBuild = issueVerifyFixMap[issue.id];
            const statusBuild = issueBuildMap[issue.id];
            if (targetBuild == null) {
                return verifyBuild != null || statusBuild == null;
            }
            if (verifyBuild != null && verifyBuild <= targetBuild) {
                return true;
            }
            if (statusBuild != null) {
                return false;
            }
            return true;
        });
    }, [buildNumber, allIssues, issueBuildMap, issueVerifyFixMap]);

    useEffect(() => {
        if (selectAllRef.current) {
            selectAllRef.current.indeterminate = somePrsSelected;
        }
    }, [somePrsSelected]);

    useEffect(() => {
        if (prs.length === 0) {
            setSelectedPrIds(new Set());
            return;
        }
        const available = new Set(prs.map(pr => pr.id));
        setSelectedPrIds(prev => {
            const next = new Set([...prev].filter(id => available.has(id)));
            return next;
        });
    }, [prs]);

    const startIssueAction = React.useCallback((issueId: number) => {
        setIssueActionIds(prev => new Set(prev).add(issueId));
    }, []);

    const endIssueAction = React.useCallback((issueId: number) => {
        setIssueActionIds(prev => {
            const next = new Set(prev);
            next.delete(issueId);
            return next;
        });
    }, []);

    const handleFixed = React.useCallback(async (id: number, number: number) => {
        setIssueActionError('');
        startIssueAction(id);
        const currentBuild = buildNumberRef.current;
        const buildTag = (currentBuild || '').trim();
        const status = buildTag ? `fixed v${buildTag}` : 'fixed';
        let closed = false;
        let commentFailed = false;
        try {
            try {
                await githubService.addComment(repo.owner, repo.name, number, status);
            } catch (e) {
                commentFailed = true;
                console.error('Failed to add fixed comment', e);
            }
            await githubService.updateIssueStatus(repo.owner, repo.name, number, 'closed');
            closed = true;
        } catch (e) {
            console.error('Failed to close issue', e);
        } finally {
            if (closed) {
                setAllIssues(prev => prev.filter(i => i.id !== id));
                setIssueBuildMap(prev => {
                    const next = { ...prev };
                    delete next[id];
                    return next;
                });
                if (commentFailed) {
                    setIssueActionError('Issue closed, but failed to add the build comment.');
                }
            } else {
                setIssueActionError('Failed to mark issue fixed. Check your token and try again.');
            }
            endIssueAction(id);
        }
    }, [repo.owner, repo.name, startIssueAction, endIssueAction]);

    const handleOpen = React.useCallback(async (id: number, number: number) => {
        setIssueActionError('');
        startIssueAction(id);
        const currentBuild = buildNumberRef.current;
        const buildTag = (currentBuild || '').trim();
        const status = buildTag ? `open v${buildTag}` : 'open';
        let commentFailed = false;
        let reopened = false;
        try {
            try {
                await githubService.addComment(repo.owner, repo.name, number, status);
            } catch (e) {
                commentFailed = true;
                console.error('Failed to add open comment', e);
            }
            await githubService.updateIssueStatus(repo.owner, repo.name, number, 'open');
            reopened = true;
        } catch (e) {
            console.error('Failed to mark issue open', e);
        } finally {
            if (reopened) {
                const targetBuild = parseBuildNumber(currentBuild);
                if (targetBuild != null) {
                    setIssueBuildMap(prev => {
                        const current = prev[id] ?? -1;
                        return { ...prev, [id]: Math.max(current, targetBuild) };
                    });
                }
                if (commentFailed) {
                    setIssueActionError('Issue reopened, but failed to add the build comment.');
                }
            } else {
                setIssueActionError('Failed to mark issue open. Check your token and try again.');
            }
            endIssueAction(id);
        }
    }, [repo.owner, repo.name, startIssueAction, endIssueAction]);

    const openBlockPrompt = React.useCallback((issue: Issue) => {
        setBlockIssue(issue);
        setBlockReason('');
        setBlockError('');
    }, []);

    const handleBlocked = async () => {
        if (!blockIssue) return;
        const issueId = blockIssue.id;
        setBlockSaving(true);
        setBlockError('');
        setIssueActionError('');
        startIssueAction(issueId);
        try {
            const buildTag = (buildNumber || '').trim();
            const reason = blockReason.trim();
            const status = buildTag ? `blocked v${buildTag}` : 'blocked';
            const body = reason ? `${status} - ${reason}` : status;
            await githubService.addComment(repo.owner, repo.name, blockIssue.number, body);
            const targetBuild = parseBuildNumber(buildNumber);
            if (targetBuild != null) {
                setIssueBuildMap(prev => {
                    const current = prev[issueId] ?? -1;
                    return { ...prev, [issueId]: Math.max(current, targetBuild) };
                });
            }
            setBlockIssue(null);
            setBlockReason('');
        } catch (e) {
            console.error('Failed to block issue', e);
            setBlockError('Failed to mark issue blocked. Check your token and try again.');
            setIssueActionError('Failed to mark issue blocked. Check your token and try again.');
        } finally {
            setBlockSaving(false);
            endIssueAction(issueId);
        }
    };

    // --- AI Analysis Handler ---
    const handleAnalyze = React.useCallback(async (issue: Issue) => {
        // Use ref to avoid stale closure or dependency loop on analyzingIds
        if (analyzingIdsRef.current.has(issue.id)) return;
        
        setAnalyzingIds(prev => new Set(prev).add(issue.id));
        try {
            const result = await aiService.analyzeIssue(issue.title, issue.description);
            setAnalysisResults(prev => ({...prev, [issue.id]: result}));
        } finally {
            setAnalyzingIds(prev => {
                const next = new Set(prev);
                next.delete(issue.id);
                return next;
            });
        }
    }, []);

    const handleAutoFix = React.useCallback((issue: Issue) => {
        if (!activeToken) {
            alert("GitHub token is required for Auto Fix.");
            return;
        }
        if (typeof window === 'undefined' || !(window as any).QAVT_AUTO_FIX?.issueCreated) {
            alert("Auto Fix plugin bridge not detected.");
            return;
        }

        const confirmFix = window.confirm(`Auto Fix issue #${issue.number}? This will trigger AI agent and publish a new build.`);
        if (!confirmFix) return;

        const currentRepo = repoRef.current;
        const currentBuild = buildNumberRef.current;

        if (!currentRepo) {
             alert("Repo information is missing.");
             return;
        }

        try {
            const payload = new URLSearchParams({
                repoId: currentRepo.id,
                repoOwner: currentRepo.owner,
                repoName: currentRepo.name,
                issueNumber: issue.number.toString(),
                title: issue.title,
                description: issue.description || '',
                buildNumber: currentBuild,
                githubToken: activeToken
            }).toString();
            (window as any).QAVT_AUTO_FIX.issueCreated(payload);
        } catch (e) {
            console.error('Failed to trigger auto fix', e);
            alert("Failed to trigger auto fix.");
        }
    }, [activeToken]);

    const toggleSelectAllPrs = () => {
        if (allPrsSelected) {
            setSelectedPrIds(new Set());
        } else {
            setSelectedPrIds(new Set(prs.map(pr => pr.id)));
        }
    };

    const togglePrSelection = (prId: number) => {
        setSelectedPrIds(prev => {
            const next = new Set(prev);
            if (next.has(prId)) {
                next.delete(prId);
            } else {
                next.add(prId);
            }
            return next;
        });
    };

    const clearPrSelection = () => {
        setSelectedPrIds(new Set());
    };

    const handleMergeSequence = async (pr: PullRequest) => {
        setPrProcessing(pr.id);
        setPrError('');
        try {
            if (pr.isDraft) await githubService.markReadyForReview(repo.owner, repo.name, pr.number);
            await githubService.approvePR(repo.owner, repo.name, pr.number);
            await githubService.mergePR(repo.owner, repo.name, pr.number);
            setPrs(prev => prev.filter(p => p.id !== pr.id));
        } catch (e) {
            console.error("Merge sequence failed", e);
            setPrError('Merge failed. Confirm repo access and PR state.');
        } finally {
            setPrProcessing(null);
        }
    };

    const handleBulkReadyAndMerge = async () => {
        if (!selectedPrs.length) return;
        setBulkProcessing(true);
        setPrError('');
        try {
            for (const pr of selectedPrs) {
                await handleMergeSequence(pr);
            }
        } finally {
            setBulkProcessing(false);
            clearPrSelection();
        }
    };

    const handleBulkReady = async () => {
        if (!selectedPrs.length) return;
        setBulkProcessing(true);
        setPrError('');
        try {
            for (const pr of selectedPrs) {
                if (pr.isDraft) {
                    await githubService.markReadyForReview(repo.owner, repo.name, pr.number);
                }
            }
            setPrs(prev => prev.map(p => selectedPrIds.has(p.id) ? { ...p, isDraft: false } : p));
        } catch (e) {
            console.error("Bulk ready failed", e);
            setPrError('Failed to mark some PRs ready. Check permissions and try again.');
        } finally {
            setBulkProcessing(false);
            clearPrSelection();
        }
    };

    const handleBulkMerge = async () => {
        if (!selectedPrs.length) return;
        setBulkProcessing(true);
        setPrError('');
        try {
            for (const pr of selectedPrs) {
                await githubService.approvePR(repo.owner, repo.name, pr.number);
                await githubService.mergePR(repo.owner, repo.name, pr.number);
            }
            setPrs(prev => prev.filter(p => !selectedPrIds.has(p.id)));
        } catch (e) {
            console.error("Bulk merge failed", e);
            setPrError('Failed to merge some PRs. Confirm repo access and PR states.');
        } finally {
            setBulkProcessing(false);
            clearPrSelection();
        }
    };

    const handleBulkClose = async () => {
        if (!selectedPrs.length) return;
        setBulkProcessing(true);
        setPrError('');
        try {
            for (const pr of selectedPrs) {
                await githubService.denyPR(repo.owner, repo.name, pr.number);
            }
            setPrs(prev => prev.filter(p => !selectedPrIds.has(p.id)));
        } catch (e) {
            console.error("Bulk close failed", e);
            setPrError('Failed to close some PRs. Check your permissions.');
        } finally {
            setBulkProcessing(false);
            clearPrSelection();
        }
    };

    const openConflictResolver = async (pr: PullRequest) => {
        setPrProcessing(pr.id);
        setPrError('');
        setConflictError('');
        setConflictLoading(true);
        try {
            const details = await githubService.getPullRequest(repo.owner, repo.name, pr.number);
            const mergeableState = (details.mergeable_state || '').toLowerCase();
            const hasConflicts =
                details.mergeable === false ||
                mergeableState === 'dirty' ||
                mergeableState === 'conflicting';

            if (!hasConflicts) {
                setPrs(prev => prev.map(p => p.id === pr.id ? { ...p, hasConflicts: false } : p));
                return;
            }

            const files = await githubService.getPullRequestFiles(repo.owner, repo.name, pr.number);
            setConflictPr(pr);
            setConflictFiles(files);
            setSelectedConflictFiles(new Set(files.map(file => file.filename)));
            setFileConflictCounts({});
            setConflictsCleared(false);
            activateConflictFile(files[0] || null);
            setResolvedFiles(new Set());
        } catch (e) {
            console.error("Resolve failed", e);
            setManualResolveIds(prev => new Set(prev).add(pr.id));
            setPrError('Resolve failed. Check permissions or try manually in GitHub.');
        } finally {
            setConflictLoading(false);
            setPrProcessing(null);
        }
    };

    useEffect(() => {
        if (!pendingConflictPr || loading || !repo) return;
        const target = prs.find(pr => pr.number === pendingConflictPr);
        if (target) {
            setPendingConflictPr(null);
            openConflictResolver(target);
            return;
        }
        if (prs.length > 0) {
            setPrError(`PR #${pendingConflictPr} not found in list.`);
            setPendingConflictPr(null);
        }
    }, [pendingConflictPr, prs, loading, repo?.id]);

    const closeConflictResolver = () => {
        setConflictPr(null);
        setConflictFiles([]);
        setActiveConflictFile(null);
        setConflictFileData(null);
        setResolvedFiles(new Set());
        setConflictSegments([]);
        setConflictSegmentOrder([]);
        setConflictCursor(0);
        setFileConflictCounts({});
        setSelectedConflictFiles(new Set());
        setConflictsCleared(false);
        setConflictError('');
        setConflictLoading(false);
        setResolvingFile(false);
    };

    const buildConflictSegments = (baseText: string, headText: string) => {
        const baseLines = baseText.length ? baseText.split('\n') : [];
        const headLines = headText.length ? headText.split('\n') : [];
        const maxCells = 2000000;
        if (baseLines.length * headLines.length > maxCells) {
            const segments: ConflictSegment[] = [{
                type: 'conflict',
                baseLines,
                headLines
            }];
            return { segments, conflictIndices: [0] };
        }

        const dp: number[][] = Array.from({ length: baseLines.length + 1 }, () =>
            new Array(headLines.length + 1).fill(0)
        );

        for (let i = baseLines.length - 1; i >= 0; i -= 1) {
            for (let j = headLines.length - 1; j >= 0; j -= 1) {
                if (baseLines[i] === headLines[j]) {
                    dp[i][j] = dp[i + 1][j + 1] + 1;
                } else {
                    dp[i][j] = Math.max(dp[i + 1][j], dp[i][j + 1]);
                }
            }
        }

        type DiffOp = { type: 'equal' | 'delete' | 'insert'; line: string };
        const ops: DiffOp[] = [];
        let i = 0;
        let j = 0;
        while (i < baseLines.length && j < headLines.length) {
            if (baseLines[i] === headLines[j]) {
                ops.push({ type: 'equal', line: baseLines[i] });
                i += 1;
                j += 1;
            } else if (dp[i + 1][j] >= dp[i][j + 1]) {
                ops.push({ type: 'delete', line: baseLines[i] });
                i += 1;
            } else {
                ops.push({ type: 'insert', line: headLines[j] });
                j += 1;
            }
        }
        while (i < baseLines.length) {
            ops.push({ type: 'delete', line: baseLines[i] });
            i += 1;
        }
        while (j < headLines.length) {
            ops.push({ type: 'insert', line: headLines[j] });
            j += 1;
        }

        const segments: ConflictSegment[] = [];
        let conflict: { baseLines: string[]; headLines: string[] } | null = null;

        const flushConflict = () => {
            if (!conflict) return;
            segments.push({
                type: 'conflict',
                baseLines: conflict.baseLines,
                headLines: conflict.headLines
            });
            conflict = null;
        };

        for (const op of ops) {
            if (op.type === 'equal') {
                flushConflict();
                const last = segments[segments.length - 1];
                if (last && last.type === 'equal') {
                    last.lines.push(op.line);
                } else {
                    segments.push({ type: 'equal', lines: [op.line] });
                }
            } else {
                if (!conflict) {
                    conflict = { baseLines: [], headLines: [] };
                }
                if (op.type === 'delete') {
                    conflict.baseLines.push(op.line);
                } else {
                    conflict.headLines.push(op.line);
                }
            }
        }
        flushConflict();

        const conflictIndices = segments.reduce((acc: number[], segment, index) => {
            if (segment.type === 'conflict') acc.push(index);
            return acc;
        }, []);

        return { segments, conflictIndices };
    };

    const buildResolvedContent = (segments: ConflictSegment[], baseEndsWithNewline: boolean) => {
        const mergedLines: string[] = [];
        for (const segment of segments) {
            if (segment.type === 'equal') {
                mergedLines.push(...segment.lines);
            } else {
                if (segment.resolution === 'incoming') {
                    mergedLines.push(...segment.headLines);
                } else if (segment.resolution === 'both') {
                    mergedLines.push(...segment.baseLines, ...segment.headLines);
                } else {
                    mergedLines.push(...segment.baseLines);
                }
            }
        }
        let merged = mergedLines.join('\n');
        if (baseEndsWithNewline && merged.length > 0) {
            merged += '\n';
        }
        return merged;
    };

    const activateConflictFile = (file: PullRequestFile | null) => {
        setActiveConflictFile(file);
        setConflictFileData(null);
        setConflictSegments([]);
        setConflictSegmentOrder([]);
        setConflictCursor(0);
        setConflictError('');
    };

    const pickNextConflictFile = (
        currentFilename: string | null,
        resolvedSet: Set<string>,
        selectedSet: Set<string>
    ) => {
        const selected = conflictFiles.filter(file => selectedSet.has(file.filename));
        if (!selected.length) return null;
        const startIndex = currentFilename ? selected.findIndex(file => file.filename === currentFilename) : -1;
        for (let offset = 1; offset <= selected.length; offset += 1) {
            const index = (startIndex + offset) % selected.length;
            const candidate = selected[index];
            if (!resolvedSet.has(candidate.filename)) {
                return candidate;
            }
        }
        return null;
    };

    const toggleConflictFileSelection = (filename: string) => {
        setSelectedConflictFiles(prev => {
            const next = new Set(prev);
            if (next.has(filename)) {
                next.delete(filename);
            } else {
                next.add(filename);
            }
            if (activeConflictFile?.filename === filename && !next.has(filename)) {
                const nextFile = pickNextConflictFile(filename, resolvedFiles, next);
                activateConflictFile(nextFile);
            }
            return next;
        });
    };

    useEffect(() => {
        if (!conflictPr || !activeConflictFile) return;
        const load = async () => {
            setConflictLoading(true);
            setConflictError('');
            try {
                const [baseFile, headFile] = await Promise.all([
                    githubService.getFileContent(repo.owner, repo.name, activeConflictFile.filename, conflictPr.targetBranch),
                    githubService.getFileContent(repo.owner, repo.name, activeConflictFile.filename, conflictPr.branch)
                ]);
                const baseContent = baseFile.content;
                const headContent = headFile.content;
                setConflictFileData({
                    base: baseContent,
                    head: headContent,
                    headSha: headFile.sha
                });
                const { segments, conflictIndices } = buildConflictSegments(baseContent, headContent);
                setConflictSegments(segments);
                setConflictSegmentOrder(conflictIndices);
                setConflictCursor(0);
                setFileConflictCounts(prev => ({ ...prev, [activeConflictFile.filename]: conflictIndices.length }));
                if (conflictIndices.length === 0) {
                    const nextResolved = new Set(resolvedFiles);
                    nextResolved.add(activeConflictFile.filename);
                    setResolvedFiles(nextResolved);
                    const nextFile = pickNextConflictFile(activeConflictFile.filename, nextResolved, selectedConflictFiles);
                    activateConflictFile(nextFile);
                    return;
                }
            } catch (e) {
                console.error('Failed to load conflict file', e);
                setConflictError('Unable to load file contents for resolution.');
            } finally {
                setConflictLoading(false);
            }
        };
        load();
    }, [conflictPr?.id, activeConflictFile?.filename]);

    const applyConflictResolution = async (mode: 'current' | 'incoming' | 'both') => {
        if (!conflictPr || !activeConflictFile || !conflictFileData) return;
        const conflictIndex = conflictSegmentOrder[conflictCursor];
        if (conflictIndex === undefined) return;

        const updatedSegments = conflictSegments.map((segment, index) => {
            if (index !== conflictIndex || segment.type !== 'conflict') return segment;
            return { ...segment, resolution: mode };
        });
        setConflictSegments(updatedSegments);

        const isLastConflict = conflictCursor >= conflictSegmentOrder.length - 1;
        if (!isLastConflict) {
            setConflictCursor(prev => prev + 1);
            return;
        }

        setResolvingFile(true);
        setConflictError('');
        try {
            const mergedContent = buildResolvedContent(
                updatedSegments,
                conflictFileData.base.endsWith('\n')
            );
            await githubService.updateFileContent(
                repo.owner,
                repo.name,
                activeConflictFile.filename,
                conflictPr.branch,
                mergedContent,
                conflictFileData.headSha,
                `Resolve conflict for ${activeConflictFile.filename}`
            );
            const nextResolved = new Set(resolvedFiles);
            nextResolved.add(activeConflictFile.filename);
            setResolvedFiles(nextResolved);
            await refreshConflictStatus();
            const nextFile = pickNextConflictFile(activeConflictFile.filename, nextResolved, selectedConflictFiles);
            activateConflictFile(nextFile);
        } catch (e) {
            console.error('Failed to resolve conflict file', e);
            setConflictError('Failed to update file. Try again or resolve in GitHub.');
        } finally {
            setResolvingFile(false);
        }
    };

    const refreshConflictStatus = async () => {
        if (!conflictPr) return;
        setConflictLoading(true);
        setConflictError('');
        try {
            const details = await githubService.getPullRequest(repo.owner, repo.name, conflictPr.number);
            const mergeableState = (details.mergeable_state || '').toLowerCase();
            const hasConflicts =
                details.mergeable === false ||
                mergeableState === 'dirty' ||
                mergeableState === 'conflicting';
            setPrs(prev => prev.map(p => p.id === conflictPr.id ? { ...p, hasConflicts } : p));
            setConflictsCleared(!hasConflicts);
            setConflictPr(prev => prev ? { ...prev, hasConflicts } : prev);
            if (!hasConflicts) {
                setManualResolveIds(prev => {
                    const next = new Set(prev);
                    next.delete(conflictPr.id);
                    return next;
                });
            }
        } catch (e) {
            console.error('Failed to refresh conflict status', e);
            setConflictError('Unable to refresh merge status. Try again.');
        } finally {
            setConflictLoading(false);
        }
    };

    const attemptAutoResolve = async () => {
        if (!conflictPr) return;
        setConflictLoading(true);
        setConflictError('');
        try {
            const success = await githubService.updateBranch(repo.owner, repo.name, conflictPr.number);
            if (!success) {
                setConflictError('Auto-resolve failed. Resolve conflicts manually or choose file-level options.');
                return;
            }
            await refreshConflictStatus();
        } catch (e) {
            console.error('Auto-resolve failed', e);
            setConflictError('Auto-resolve failed. Try again or resolve manually.');
        } finally {
            setConflictLoading(false);
        }
    };

    const mergeResolvedConflictPr = async () => {
        if (!conflictPr) return;
        setConflictLoading(true);
        setConflictError('');
        try {
            if (conflictPr.isDraft) {
                await githubService.markReadyForReview(repo.owner, repo.name, conflictPr.number);
            }
            await githubService.approvePR(repo.owner, repo.name, conflictPr.number);
            await githubService.mergePR(repo.owner, repo.name, conflictPr.number);
            setPrs(prev => prev.filter(p => p.id !== conflictPr.id));
            closeConflictResolver();
        } catch (e) {
            console.error('Merge after conflict resolution failed', e);
            setConflictError('Merge failed. Confirm review status and try again.');
        } finally {
            setConflictLoading(false);
        }
    };

    const selectedConflictFileList = conflictFiles.filter(file => selectedConflictFiles.has(file.filename));
    const unresolvedSelectedFiles = selectedConflictFileList.filter(file => !resolvedFiles.has(file.filename));
    const activeConflictSegmentIndex = conflictSegmentOrder[conflictCursor];
    const activeConflictSegment = activeConflictSegmentIndex !== undefined
        ? conflictSegments[activeConflictSegmentIndex]
        : null;
    const activeConflictCount = activeConflictFile
        ? (fileConflictCounts[activeConflictFile.filename] ?? conflictSegmentOrder.length)
        : 0;
    const mergeReady = (selectedConflictFileList.length > 0 && unresolvedSelectedFiles.length === 0) || conflictsCleared;

    const handleClosePR = async (pr: PullRequest) => {
        setPrs(prev => prev.filter(p => p.id !== pr.id));
        setUndoPr({ id: pr.id, pr });
        await githubService.denyPR(repo.owner, repo.name, pr.number);
        setTimeout(() => setUndoPr(null), 5000);
    };

    const handleUndoClose = async () => {
        if (!undoPr) return;
        setPrs(prev => [...prev, undoPr.pr]);
        setUndoPr(null);
        await githubService.updateIssueStatus(repo.owner, repo.name, undoPr.pr.number, 'open'); // PRs use issue API for state sometimes or specific PR update
    };

    if (!repo) return <div className="p-10 text-center">Please select a repository.</div>;
    if (!activeToken) return <div className="p-10 text-center">Missing GitHub token. Configure a global token or enable a repo token.</div>;

    return (
        <div className="relative flex h-full min-h-screen w-full flex-col overflow-x-hidden pb-24">
            <header className="sticky top-0 z-20 bg-background-light/95 dark:bg-background-dark/95 backdrop-blur-md border-b border-gray-200 dark:border-white/5 px-4 py-3 flex items-center gap-3">
                <div className="flex items-center gap-3 shrink-0">
                    <button onClick={() => navigate('/')} aria-label="Go back to home" className="p-1 rounded-full hover:bg-black/5 dark:hover:bg-white/10 transition-colors">
                        <span className="material-symbols-outlined text-slate-900 dark:text-white">arrow_back</span>
                    </button>
                    <div>
                        <h1 className="text-lg font-bold leading-none tracking-tight">QA Dashboard</h1>
                        <p className="text-xs text-gray-500 dark:text-gray-400 font-medium">{repo?.displayName || repo?.name}</p>
                    </div>
                </div>
                <div className="flex-1 min-w-0 flex items-center justify-end">
                    <div className="flex items-center gap-2 overflow-x-auto py-1">
                        <a
                            href={repo ? `https://github.com/${repo.owner}/${repo.name}/projects` : '#'}
                            target="_blank"
                            rel="noopener noreferrer"
                            className="shrink-0 inline-flex items-center gap-1 rounded-lg border border-gray-200 dark:border-white/10 bg-white/70 dark:bg-surface-dark-lighter backdrop-blur-sm px-2.5 py-1.5 text-[10px] sm:text-[11px] font-bold text-gray-700 dark:text-gray-200 hover:text-primary transition-colors whitespace-nowrap"
                        >
                            <span className="material-symbols-outlined text-[14px]">dashboard</span>
                            Projects
                        </a>
                        <div className="shrink-0 flex items-center bg-gray-200 dark:bg-surface-dark-lighter rounded-lg p-0.5 border border-transparent dark:border-white/10 whitespace-nowrap">    
                            <button onClick={() => setTab('issues')} className={`px-3 py-1.5 rounded-md text-xs font-bold transition-all ${tab === 'issues' ? 'bg-primary text-black shadow-sm' : 'text-gray-500 dark:text-gray-400 hover:text-slate-900 dark:hover:text-white'}`}>Issues</button>
                            <button onClick={() => setTab('prs')} className={`px-3 py-1.5 rounded-md text-xs font-bold transition-all ${tab === 'prs' ? 'bg-primary text-black shadow-sm' : 'text-gray-500 dark:text-gray-400 hover:text-slate-900 dark:hover:text-white'}`}>PRs</button>
                            <button onClick={() => setTab('tests')} className={`px-3 py-1.5 rounded-md text-xs font-bold transition-all ${tab === 'tests' ? 'bg-primary text-black shadow-sm' : 'text-gray-500 dark:text-gray-400 hover:text-slate-900 dark:hover:text-white'}`}>Tests</button>
                        </div>
                    </div>
                </div>
            </header>

            <section className="px-4 py-3 space-y-3">
                 <div className="flex flex-col gap-3 lg:flex-row lg:items-end">
                    <div className="flex-1 space-y-1.5">
                        <label htmlFor="dashboard-target-build" className="text-xs font-semibold text-gray-500 dark:text-[#9db99f] uppercase tracking-wider">Target Build</label>
                        <div className="relative flex items-center gap-2">
                            <span className="absolute left-3 material-symbols-outlined text-gray-400 text-[20px]">tag</span>
                            <input id="dashboard-target-build" className="w-full bg-white dark:bg-input-dark backdrop-blur-sm border-gray-200 dark:border-white/5 rounded-lg py-3 pl-10 pr-3 font-mono font-bold text-lg focus:ring-2 focus:ring-primary focus:border-primary transition-all text-slate-900 dark:text-white" type="text" value={buildNumber} onChange={(e) => { const value = e.target.value; setBuildNumber(value); persistBuildNumber(value); }} maxLength={20}/>
                            <div className="flex items-center gap-2">
                                <button
                                    onClick={() => handleSync(false)}
                                    disabled={loading}
                                    aria-label="Sync with GitHub"
                                    className="bg-white dark:bg-surface-dark-lighter backdrop-blur-sm border border-gray-200 dark:border-white/5 rounded-lg px-3 py-3 hover:text-primary transition-colors flex items-center gap-1 disabled:opacity-50"
                                >
                                    <span className={`material-symbols-outlined ${loading && syncType === 'github' ? 'animate-spin' : ''}`}>sync</span>
                                    <span className="text-xs font-bold hidden sm:inline">Sync</span>
                                </button>
                                <button
                                    onClick={() => handleSync(true)}
                                    disabled={loading}
                                    aria-label="Sync with Store"
                                    className="bg-white dark:bg-surface-dark-lighter backdrop-blur-sm border border-gray-200 dark:border-white/5 rounded-lg px-3 py-3 hover:text-primary transition-colors flex items-center gap-1 disabled:opacity-50"
                                >
                                    {loading && syncType === 'store' ? (
                                        <div className="size-5 border-2 border-current border-t-transparent rounded-full animate-spin"></div>
                                    ) : (
                                        <span className="material-symbols-outlined">system_update_alt</span>
                                    )}
                                    <span className="text-xs font-bold hidden sm:inline">Store</span>
                                </button>
                            </div>
                        </div>
                    </div>
                    <div className="flex-1 space-y-1.5">
                         <label htmlFor="dashboard-app-variant" className="text-xs font-semibold text-gray-500 dark:text-[#9db99f] uppercase tracking-wider">App Variant</label>
                         <select
                             id="dashboard-app-variant"
                             value={activeApp?.id || ''}
                             onChange={(e) => setActiveAppId(e.target.value)}
                             className="w-full bg-white dark:bg-input-dark backdrop-blur-sm border-gray-200 dark:border-white/5 rounded-lg h-[54px] px-3 font-medium text-sm text-slate-900 dark:text-white focus:ring-primary focus:border-primary"
                         >
                             {repo?.apps.map(app => (
                                 <option key={app.id} value={app.id}>
                                     {app.name} ({app.platform})
                                 </option>
                             ))}
                         </select>
                    </div>
                 </div>
                 <div className="rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-surface-dark-lighter/80 p-3 shadow-sm">
                     <div className="flex items-center justify-between">
                         <div>
                             <p className="text-xs font-bold uppercase tracking-wider text-gray-500 dark:text-gray-400">Test Builds</p>
                             <p className="text-[11px] text-gray-400">Jump to tests by build and platform.</p>
                         </div>
                         <button
                             onClick={() => setTab('tests')}
                             className="rounded-lg border border-gray-200 dark:border-white/10 bg-white/70 dark:bg-surface-dark-lighter backdrop-blur-sm px-2.5 py-1.5 text-[10px] font-bold text-gray-700 dark:text-gray-200 hover:text-primary transition-colors"
                         >
                             Open Tests
                         </button>
                     </div>
                     <div className="mt-3 space-y-2">
                         {repo?.apps.map(app => (
                             <div key={app.id} className="flex flex-col gap-2 rounded-lg border border-gray-100 dark:border-white/5 bg-white/80 dark:bg-black/20 p-2 sm:flex-row sm:items-center sm:justify-between">
                                 <div>
                                     <p className="text-sm font-semibold text-slate-900 dark:text-white">{app.name} <span className="text-[11px] text-gray-500">({app.platform})</span></p>
                                     <p className="text-[11px] text-gray-500">Build #{app.buildNumber || '—'}</p>
                                 </div>
                                 <div className="flex items-center gap-2">
                                     <button
                                         onClick={() => openTestsForApp(app)}
                                         className="rounded-lg border border-gray-200 dark:border-white/10 px-3 py-1.5 text-[11px] font-semibold text-gray-700 dark:text-gray-200 hover:text-primary transition-colors"
                                     >
                                         View Tests
                                     </button>
                                     {app.playStoreUrl && isValidUrl(app.playStoreUrl) ? (
                                         <a
                                             href={app.playStoreUrl}
                                             target="_blank"
                                             rel="noopener noreferrer"
                                             className="rounded-lg bg-primary/10 px-3 py-1.5 text-[11px] font-semibold text-primary hover:bg-primary/20"
                                         >
                                             Open Test
                                         </a>
                                     ) : (
                                         <span className="text-[10px] uppercase tracking-wide text-gray-400">No test url</span>
                                     )}
                                 </div>
                             </div>
                         ))}
                     </div>
                 </div>
            </section>
            <div className="h-px w-full bg-gray-200 dark:bg-white/5 mb-4"></div>

            <main className="flex-1 px-4 space-y-3">
                {undoPr && (
                    <div className="flex items-center justify-between bg-gray-900 text-white p-3 rounded-lg animate-slide-in-right shadow-xl">
                        <span className="text-sm">PR #{undoPr.pr.number} closed.</span>
                        <button onClick={handleUndoClose} className="text-primary font-bold text-sm uppercase tracking-wider">Undo</button>
                    </div>
                )}
                
                {loading && <div className="text-center py-10"><div className="inline-block size-8 border-2 border-primary border-t-transparent rounded-full animate-spin"></div></div>}

                {!loading && tab === 'issues' && (
                    <>
                        {syncError && <div className="mb-2 rounded-lg border border-red-500/30 bg-red-500/10 text-red-200 text-xs px-3 py-2">{syncError}</div>}
                        {issueActionError && <div className="mb-2 rounded-lg border border-amber-500/30 bg-amber-500/10 text-amber-200 text-xs px-3 py-2">{issueActionError}</div>}
                        <div className="flex justify-between items-center pb-1">
                             <h2 className="text-sm font-bold text-slate-900 dark:text-white">Pending Verification</h2>
                             <span className="text-[11px] text-gray-400">{issues.length} remaining</span>
                        </div>
                        {issues.length === 0 ? (
                            <div className="text-center py-10 opacity-50"><span className="material-symbols-outlined text-6xl text-gray-600">check_circle</span><p className="mt-4 text-gray-400">All cleared for build {buildNumber}!</p></div>
                        ) : (
                            issues.map(issue => (
                                <IssueCard
                                    key={issue.id}
                                    issue={issue}
                                    repoId={repo.id}
                                    isBusy={issueActionIds.has(issue.id)}
                                    isAnalyzing={analyzingIds.has(issue.id)}
                                    analysisResult={analysisResults[issue.id]}
                                    onFixed={handleFixed}
                                    onOpen={handleOpen}
                                    onBlock={openBlockPrompt}
                                    onAnalyze={handleAnalyze}
                                    onAutoFix={handleAutoFix}
                                    autoFixAvailable={autoFixAvailable}
                                />
                            ))
                        )}
                    </>
                )}

                {!loading && tab === 'prs' && (
                    <div className="space-y-3">
                        {prError && <div className="rounded-lg border border-red-500/30 bg-red-500/10 text-red-200 text-xs px-3 py-2">{prError}</div>}
                        {prs.length > 0 && (
                            <div className="flex items-center justify-between rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-surface-dark-lighter/80 p-3 shadow-sm">
                                <div className="flex items-center gap-3">
                                    <input
                                        ref={selectAllRef}
                                        type="checkbox"
                                        className="size-4 accent-primary"
                                        checked={allPrsSelected}
                                        onChange={toggleSelectAllPrs}
                                    />
                                    <div>
                                        <p className="text-sm font-bold text-slate-900 dark:text-white">Select all PRs</p>
                                        <p className="text-[11px] text-gray-500">
                                            {selectedPrIds.size > 0
                                                ? `${selectedPrIds.size} of ${prs.length} selected`
                                                : `${prs.length} total`}
                                        </p>
                                    </div>
                                </div>
                                <button
                                    onClick={clearPrSelection}
                                    disabled={selectedPrIds.size === 0}
                                    className="text-xs font-bold uppercase tracking-wider text-gray-500 disabled:opacity-40 hover:text-primary"
                                >
                                    Clear
                                </button>
                            </div>
                        )}
                        {prs.map(pr => {
                             const isProcessing = prProcessing === pr.id;
                             const isManual = manualResolveIds.has(pr.id);

                             return (
                                <div key={pr.id} className="group relative bg-white dark:bg-surface-dark backdrop-blur-md rounded-xl p-3 border border-gray-200 dark:border-white/10 shadow-sm transition-all animate-fade-in">
                                    <div className="flex items-start gap-3 mb-1.5">
                                        <input
                                            type="checkbox"
                                            className="mt-1 size-4 accent-primary"
                                            checked={selectedPrIds.has(pr.id)}
                                            onChange={() => togglePrSelection(pr.id)}
                                        />
                                        <div className="flex-1">
                                            <div className="flex justify-between items-start">
                                                <div className="pr-2">
                                                    <div className="flex items-center gap-2 mb-1">
                                                        {pr.isDraft && <span className="px-1.5 py-0.5 rounded text-[10px] font-bold bg-gray-200 dark:bg-gray-700 text-gray-600 dark:text-gray-300 uppercase tracking-wide">Draft</span>}
                                                        {pr.hasConflicts && <span className="px-1.5 py-0.5 rounded text-[10px] font-bold bg-red-500/10 text-red-500 uppercase tracking-wide flex items-center gap-1"><span className="material-symbols-outlined text-[12px]">warning</span>Conflict</span>}
                                                    </div>
                                                    <h3 className="text-slate-900 dark:text-white font-bold text-sm leading-tight">{pr.title}</h3>
                                                    <div className="text-xs text-gray-500 mt-1">#{pr.number} - {pr.author.name}</div>
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                    <div className="flex items-center gap-2 text-xs text-gray-500 mb-3 font-mono bg-gray-50 dark:bg-white/5 p-1.5 rounded-lg border border-gray-200 dark:border-white/5">
                                        <span className="text-slate-700 dark:text-gray-300">{pr.branch}</span><span className="material-symbols-outlined text-[12px]">arrow_forward</span><span className="text-slate-700 dark:text-gray-300">{pr.targetBranch}</span>
                                    </div>
                                    
                                    <div className="flex gap-2">
                                        {pr.hasConflicts ? (
                                            <>
                                                <button 
                                                    disabled={isProcessing}
                                                    onClick={() => openConflictResolver(pr)}
                                                    className="flex-1 bg-amber-600/20 text-amber-700 dark:text-amber-500 border border-amber-600/30 font-semibold text-xs py-2 rounded-lg flex items-center justify-center gap-1.5 active:scale-[0.98] disabled:opacity-50"
                                                >
                                                    {isProcessing ? 'Loading...' : (
                                                        <>
                                                            <span className="material-symbols-outlined text-[16px]">build</span>
                                                            Resolve Conflicts
                                                        </>
                                                    )}
                                                </button>
                                                {isManual && (
                                                    <a
                                                        href={`https://github.com/${repo.owner}/${repo.name}/pull/${pr.number}/conflicts`}
                                                        target="_blank"
                                                        rel="noopener noreferrer"
                                                        className="flex-1 bg-amber-600/10 text-amber-700 dark:text-amber-400 border border-amber-600/20 font-semibold text-xs py-2 rounded-lg flex items-center justify-center gap-1.5 active:scale-[0.98] hover:bg-amber-600/20"
                                                    >
                                                        <span className="material-symbols-outlined text-[16px]">open_in_new</span>
                                                        Open in GitHub
                                                    </a>
                                                )}
                                            </>
                                        ) : (
                                            <button 
                                                disabled={isProcessing}
                                                onClick={() => handleMergeSequence(pr)} 
                                                className={`flex-1 font-bold text-xs py-2 rounded-lg flex items-center justify-center gap-1.5 active:scale-[0.98] disabled:opacity-50 transition-colors ${
                                                    pr.isDraft 
                                                    ? 'bg-blue-600 text-white hover:bg-blue-700' 
                                                    : 'bg-primary text-black hover:bg-primary-hover'
                                                }`}
                                            >
                                                {isProcessing ? 'Processing...' : (
                                                    <>
                                                        <span className="material-symbols-outlined text-[18px]">{pr.isDraft ? 'rocket_launch' : 'merge'}</span>
                                                        {pr.isDraft ? 'Ready & Merge' : 'Merge'}
                                                    </>
                                                )}
                                            </button>
                                        )}
                                        <button disabled={isProcessing} onClick={() => handleClosePR(pr)} aria-label="Close pull request" className="px-4 bg-red-500/10 text-red-600 dark:text-red-500 border border-red-500/20 rounded-lg flex items-center justify-center active:scale-[0.98] hover:bg-red-500/20"><span className="material-symbols-outlined text-[20px]">close</span></button>
                                    </div>
                                </div>
                             )
                        })}
                    </div>
                )}

                {tab === 'tests' && (
                    <div className="space-y-3">
                        <div className="flex flex-col gap-3">
                            <div className="bg-white dark:bg-surface-dark-lighter rounded-xl p-3 border border-gray-200 dark:border-white/5 space-y-3">
                                <h3 className="font-bold text-sm text-slate-900 dark:text-white uppercase tracking-wider">Add Manual Test</h3>
                                <div className="flex gap-2">
                                    <input
                                        type="text"
                                        value={newTestDesc}
                                        onChange={e => setNewTestDesc(e.target.value)}
                                        onKeyDown={e => e.key === 'Enter' && handleAddTest()}
                                        placeholder="e.g. Verify login with invalid credentials"
                                        maxLength={255}
                                        className="flex-1 bg-background-light dark:bg-background-dark border-transparent rounded-lg px-3 py-2 text-sm focus:ring-primary focus:border-primary text-slate-900 dark:text-white"
                                    />
                                    <button onClick={handleAddTest} disabled={!newTestDesc.trim()} className="bg-black/5 dark:bg-white/10 hover:bg-black/10 dark:hover:bg-white/20 text-slate-900 dark:text-white px-4 rounded-lg font-bold text-sm disabled:opacity-50">Add</button>
                                </div>
                            </div>

                            <button
                                onClick={handleGenerateTests}
                                disabled={generatingTests}
                                className="w-full bg-primary/10 hover:bg-primary/20 text-primary border border-primary/20 rounded-xl p-3 flex items-center justify-center gap-2 font-bold text-sm transition-colors disabled:opacity-50"
                            >
                                {generatingTests ? (
                                    <span className="size-4 border-2 border-current border-t-transparent rounded-full animate-spin"></span>
                                ) : (
                                    <span className="material-symbols-outlined text-lg">smart_toy</span>
                                )}
                                Generate AI Test Plan
                            </button>
                        </div>

                        <div className="space-y-2">
                             <div className="flex justify-between items-center pb-1">
                                 <h2 className="text-sm font-bold text-slate-900 dark:text-white">Verification Checklist</h2>
                                 <span className="text-[11px] text-gray-400">Build #{buildNumber}</span>
                             </div>

                            {tests.length === 0 ? (
                                <div className="text-center py-10 opacity-50">
                                    <span className="material-symbols-outlined text-5xl mb-2">playlist_add_check</span>
                                    <p className="text-sm font-medium">No tests defined yet.</p>
                                </div>
                            ) : (
                                tests.map(test => {
                                    const activeBuild = (buildNumber || '').toString().trim();
                                    const checkedBuilds = getCheckedBuilds(test);
                                    const isChecked = activeBuild ? checkedBuilds.includes(activeBuild) : false;
                                    const latestChecked = getLatestCheckedBuild(test);
                                    return (
                                        <div key={test.id} onClick={() => toggleTest(test.id)} className={`group cursor-pointer flex items-start gap-2 p-2 rounded-xl border transition-all ${isChecked ? 'bg-primary/5 border-primary/20' : 'bg-white dark:bg-surface-dark-lighter border-gray-200 dark:border-white/5 hover:border-primary/30'}`}>
                                            <div className={`mt-0.5 flex-shrink-0 size-5 rounded border flex items-center justify-center transition-colors ${isChecked ? 'bg-primary border-primary text-black' : 'bg-transparent border-gray-400 dark:border-gray-500 group-hover:border-primary'}`}>
                                                {isChecked && <span className="material-symbols-outlined text-sm font-bold">check</span>}
                                            </div>
                                            <div className="flex-1">
                                                <p className={`text-sm ${isChecked ? 'text-gray-500 line-through' : 'text-slate-900 dark:text-white'}`}>{test.description}</p>
                                                {latestChecked && !isChecked && (
                                                    <p className="text-[10px] text-gray-400 mt-1">Last checked on build #{latestChecked}</p>
                                                )}
                                            </div>
                                        </div>
                                    );
                                })
                            )}
                        </div>
                    </div>
                )}
            </main>
            {tab === 'prs' && selectedPrIds.size > 0 && (
                <div className="fixed bottom-20 left-0 right-0 z-40 px-4">
                    <div className="mx-auto max-w-md rounded-2xl border border-white/10 bg-surface-dark/95 p-3 text-white shadow-2xl backdrop-blur-md">
                        <div className="flex items-center justify-between">
                            <span className="text-sm font-semibold">{selectedPrIds.size} selected</span>
                            <button onClick={clearPrSelection} className="text-[11px] font-bold uppercase tracking-wider text-gray-300 hover:text-primary">
                                Clear
                            </button>
                        </div>
                        <div className="mt-2 flex items-center gap-2 overflow-x-auto">
                            <button
                                onClick={handleBulkReadyAndMerge}
                                disabled={bulkProcessing}
                                className="whitespace-nowrap rounded-lg bg-primary px-3 py-2 text-xs font-bold text-black disabled:opacity-50"
                            >
                                {bulkProcessing ? 'Working...' : 'Ready + Merge'}
                            </button>
                            <button
                                onClick={handleBulkReady}
                                disabled={bulkProcessing}
                                className="whitespace-nowrap rounded-lg border border-white/15 px-3 py-2 text-xs font-semibold text-gray-200 disabled:opacity-50"
                            >
                                Ready
                            </button>
                            <button
                                onClick={handleBulkMerge}
                                disabled={bulkProcessing}
                                className="whitespace-nowrap rounded-lg border border-white/15 px-3 py-2 text-xs font-semibold text-gray-200 disabled:opacity-50"
                            >
                                Merge
                            </button>
                            <button
                                onClick={handleBulkClose}
                                disabled={bulkProcessing}
                                className="whitespace-nowrap rounded-lg border border-red-500/40 px-3 py-2 text-xs font-semibold text-red-300 disabled:opacity-50"
                            >
                                Close
                            </button>
                        </div>
                    </div>
                </div>
            )}
            <Link to={`/quick-issue?repo=${repo?.id}&build=${encodeURIComponent(buildNumber)}`} aria-label="Create new issue" className="fixed bottom-24 right-4 z-30 flex h-14 w-14 items-center justify-center rounded-full bg-primary text-black shadow-[0_4px_16px_rgba(19,236,37,0.4)] active:scale-90 transition-transform hover:scale-105"><span className="material-symbols-outlined text-3xl">add</span></Link>
            <BottomNav onNotesClick={onNotesClick} />
            {blockIssue && (
                <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 backdrop-blur-sm">
                    <div className="w-full max-w-md rounded-xl border border-white/10 bg-surface-dark p-5 text-white shadow-2xl">
                        <div className="flex items-center justify-between">
                            <h3 className="text-lg font-bold">Block Issue #{blockIssue.number}</h3>
                            <button onClick={() => setBlockIssue(null)} aria-label="Close block dialog" className="rounded-full p-1 hover:bg-white/10">
                                <span className="material-symbols-outlined">close</span>
                            </button>
                        </div>
                        <p className="mt-1 text-xs text-gray-400">Optional reason (saved to the issue as a comment).</p>
                        <textarea
                            value={blockReason}
                            onChange={(e) => setBlockReason(e.target.value)}
                            className="mt-3 w-full min-h-[120px] rounded-lg bg-input-dark p-3 text-sm text-white"
                            placeholder="e.g. Waiting on backend fix / missing access"
                            maxLength={1000}
                        />
                        {blockError && <div className="mt-3 rounded-lg border border-red-500/30 bg-red-500/10 px-3 py-2 text-xs text-red-200">{blockError}</div>}
                        <div className="mt-4 flex items-center justify-end gap-2">
                            <button onClick={() => setBlockIssue(null)} className="rounded-lg border border-white/10 px-3 py-2 text-xs font-semibold text-gray-200 hover:bg-white/10">Cancel</button>
                            <button onClick={handleBlocked} disabled={blockSaving} className="rounded-lg bg-red-600 px-3 py-2 text-xs font-semibold text-white disabled:opacity-50">
                                {blockSaving ? 'Blocking...' : 'Mark Blocked'}
                            </button>
                        </div>
                    </div>
                </div>
            )}
            {conflictPr && (
                <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 backdrop-blur-sm">
                    <div className="flex h-[85vh] w-full max-w-5xl flex-col rounded-2xl border border-white/10 bg-surface-dark text-white shadow-2xl">
                        <div className="flex items-center justify-between border-b border-white/10 px-5 py-4">
                            <div>
                                <h3 className="text-lg font-bold">Resolve Conflicts</h3>
                                <p className="text-xs text-gray-400">PR #{conflictPr.number} - {conflictPr.title}</p>
                                <p className="text-[11px] font-semibold uppercase tracking-wide text-amber-400">Conflicts detected</p>
                            </div>
                            <button onClick={closeConflictResolver} aria-label="Close conflict resolver" className="rounded-full p-1 hover:bg-white/10">
                                <span className="material-symbols-outlined">close</span>
                            </button>
                        </div>
                        <div className="flex flex-1 overflow-hidden">
                            <div className="w-72 border-r border-white/10 overflow-y-auto">
                                <div className="border-b border-white/5 px-4 py-3">
                                    <p className="text-xs font-semibold text-gray-300">Changed files</p>
                                    <p className="text-[11px] text-gray-500">
                                        {selectedConflictFileList.length} selected - {resolvedFiles.size} resolved
                                    </p>
                                </div>
                                {conflictFiles.length === 0 && (
                                    <div className="p-4 text-xs text-gray-400">No changed files found.</div>
                                )}
                                {conflictFiles.map(file => {
                                    const isActive = activeConflictFile?.filename === file.filename;
                                    const isResolved = resolvedFiles.has(file.filename);
                                    const isSelected = selectedConflictFiles.has(file.filename);
                                    const conflictCount = fileConflictCounts[file.filename];
                                    const hasConflicts = conflictCount === undefined ? true : conflictCount > 0;
                                    const conflictLabel = conflictCount === undefined
                                        ? 'Checking...'
                                        : conflictCount === 0
                                            ? 'No conflicts'
                                            : `${conflictCount} conflict${conflictCount === 1 ? '' : 's'}`;
                                    return (
                                        <button
                                            key={file.filename}
                                            onClick={() => {
                                                if (!isSelected) {
                                                    setSelectedConflictFiles(prev => new Set(prev).add(file.filename));
                                                }
                                                activateConflictFile(file);
                                            }}
                                            className={`w-full px-4 py-3 text-left text-xs border-b border-white/5 transition-colors ${
                                                isActive ? 'bg-white/10' : 'hover:bg-white/5'
                                            } ${isSelected ? '' : 'opacity-50'}`}
                                        >
                                            <div className="flex items-start justify-between gap-2">
                                                <div className="flex items-start gap-2">
                                                    <input
                                                        type="checkbox"
                                                        checked={isSelected}
                                                        onChange={() => toggleConflictFileSelection(file.filename)}
                                                        onClick={(e) => e.stopPropagation()}
                                                        className="mt-0.5 size-3 accent-amber-500"
                                                    />
                                                    <span className={`line-clamp-2 ${
                                                        isActive ? 'text-white' : hasConflicts ? 'text-amber-200' : 'text-gray-300'
                                                    }`}>{file.filename}</span>
                                                </div>
                                                {isResolved && (
                                                    <span className="rounded-full bg-primary/20 px-2 py-0.5 text-[10px] font-bold text-primary">Resolved</span>
                                                )}
                                            </div>
                                            <div className="mt-1 flex items-center justify-between text-[10px] uppercase">
                                                <span className="text-gray-500">{file.status}</span>
                                                <span className={hasConflicts ? 'text-amber-400' : 'text-gray-500'}>
                                                    {conflictLabel}
                                                </span>
                                            </div>
                                        </button>
                                    );
                                })}
                            </div>
                            <div className="flex flex-1 flex-col overflow-hidden">
                                <div className="flex items-center justify-between border-b border-white/10 px-4 py-3">
                                    <div>
                                        <div className="text-xs text-gray-400">
                                            {activeConflictFile ? activeConflictFile.filename : 'Select a file to resolve'}
                                        </div>
                                        {activeConflictFile && activeConflictCount > 0 && (
                                            <div className="text-[11px] font-semibold text-amber-400">
                                                Conflict {Math.min(conflictCursor + 1, activeConflictCount)} of {activeConflictCount}
                                            </div>
                                        )}
                                    </div>
                                    <div className="flex items-center gap-2">
                                        <button
                                            onClick={() => applyConflictResolution('current')}
                                            disabled={!activeConflictSegment || resolvingFile}
                                            className="rounded-lg border border-white/10 px-3 py-1.5 text-[11px] font-semibold text-gray-200 disabled:opacity-40"
                                        >
                                            Accept Current
                                        </button>
                                        <button
                                            onClick={() => applyConflictResolution('incoming')}
                                            disabled={!activeConflictSegment || resolvingFile}
                                            className="rounded-lg border border-white/10 px-3 py-1.5 text-[11px] font-semibold text-gray-200 disabled:opacity-40"
                                        >
                                            Accept Incoming
                                        </button>
                                        <button
                                            onClick={() => applyConflictResolution('both')}
                                            disabled={!activeConflictSegment || resolvingFile}
                                            className="rounded-lg bg-amber-400 px-3 py-1.5 text-[11px] font-bold text-black disabled:opacity-40"
                                        >
                                            Accept Both
                                        </button>
                                    </div>
                                </div>
                                <div className="flex-1 overflow-y-auto p-4 space-y-4">
                                    {conflictError && (
                                        <div className="rounded-lg border border-red-500/30 bg-red-500/10 px-3 py-2 text-xs text-red-200">
                                            {conflictError}
                                        </div>
                                    )}
                                    {conflictLoading && (
                                        <div className="flex items-center gap-2 text-xs text-gray-400">
                                            <span className="size-3 border-2 border-gray-300 border-t-transparent rounded-full animate-spin"></span>
                                            Loading file contents...
                                        </div>
                                    )}
                                    {!conflictLoading && activeConflictFile && conflictFileData && activeConflictSegment && activeConflictSegment.type === 'conflict' && (
                                        <div className="grid gap-4 md:grid-cols-2">
                                            <div>
                                                <p className="text-[11px] font-bold uppercase text-gray-400 mb-2">Current change</p>
                                                <pre className="whitespace-pre-wrap rounded-lg bg-black/40 p-3 text-[11px] text-gray-200">
                                                    {activeConflictSegment.baseLines.length
                                                        ? activeConflictSegment.baseLines.join('\n')
                                                        : '[no lines]'}
                                                </pre>
                                            </div>
                                            <div>
                                                <p className="text-[11px] font-bold uppercase text-gray-400 mb-2">Incoming change</p>
                                                <pre className="whitespace-pre-wrap rounded-lg bg-black/40 p-3 text-[11px] text-gray-200">
                                                    {activeConflictSegment.headLines.length
                                                        ? activeConflictSegment.headLines.join('\n')
                                                        : '[no lines]'}
                                                </pre>
                                            </div>
                                        </div>
                                    )}
                                    {!conflictLoading && activeConflictFile && conflictFileData && conflictSegmentOrder.length === 0 && (
                                        <div className="text-xs text-gray-400">
                                            No conflict blocks detected for this file.
                                        </div>
                                    )}
                                    {!conflictLoading && activeConflictFile && conflictFileData && !activeConflictSegment && conflictSegmentOrder.length > 0 && (
                                        <div className="text-xs text-gray-400">
                                            All conflict blocks resolved for this file.
                                        </div>
                                    )}
                                    {!conflictLoading && !activeConflictFile && (
                                        <div className="text-xs text-gray-400">Select a file to view and resolve conflicts.</div>
                                    )}
                                </div>
                            </div>
                        </div>
                        <div className="flex items-center justify-between border-t border-white/10 px-5 py-3">
                            <span className="text-xs text-gray-400">
                                {unresolvedSelectedFiles.length === 0
                                    ? 'All selected files resolved'
                                    : `${unresolvedSelectedFiles.length} files remaining`}
                            </span>
                            <div className="flex items-center gap-2">
                                <button
                                    onClick={mergeResolvedConflictPr}
                                    disabled={!mergeReady || conflictLoading}
                                    className="rounded-lg bg-emerald-400 px-3 py-2 text-xs font-semibold text-black disabled:opacity-40"
                                >
                                    Merge PR
                                </button>
                                <button
                                    onClick={attemptAutoResolve}
                                    disabled={conflictLoading}
                                    className="rounded-lg border border-white/10 px-3 py-2 text-xs font-semibold text-gray-200 disabled:opacity-40"
                                >
                                    Try Auto-Resolve
                                </button>
                                <button
                                    onClick={refreshConflictStatus}
                                    disabled={conflictLoading}
                                    className="rounded-lg border border-white/10 px-3 py-2 text-xs font-semibold text-gray-200 disabled:opacity-40"
                                >
                                    Recheck Mergeable
                                </button>
                                <a
                                    href={`https://github.com/${repo.owner}/${repo.name}/pull/${conflictPr.number}/conflicts`}
                                    target="_blank"
                                    rel="noopener noreferrer"
                                    className="rounded-lg bg-white/10 px-3 py-2 text-xs font-semibold text-gray-100 hover:bg-white/20"
                                >
                                    Open in GitHub
                                </a>
                            </div>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

// 4. Issue Detail Page
const IssueDetailPage = ({ repos, globalSettings, onNotesClick }: { repos: Repository[], globalSettings: GlobalSettings, onNotesClick: () => void }) => {
    const navigate = useNavigate();
    const { repoId, issueNumber } = useParams();
    const repo = repos.find(r => r.id === repoId);
    const token = repo ? resolveGithubToken(repo, globalSettings) : '';
    const [issue, setIssue] = useState<Issue | null>(null);
    const [comments, setComments] = useState<Comment[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [commentDraft, setCommentDraft] = useState('');
    const [commentSaving, setCommentSaving] = useState(false);
    const [commentError, setCommentError] = useState('');

    useEffect(() => {
        let isMounted = true;
        const loadIssue = async () => {
            if (!repo) {
                setError('Repository not found.');
                setLoading(false);
                return;
            }
            if (!token) {
                setError('Missing GitHub token. Configure a global or repo token.');
                setLoading(false);
                return;
            }
            const number = Number(issueNumber);
            if (!number) {
                setError('Invalid issue number.');
                setLoading(false);
                return;
            }

            setLoading(true);
            setError('');
            try {
                githubService.initialize(token);
                const issueData = await githubService.getIssue(repo.owner, repo.name, number);
                if (isMounted) {
                    setIssue(issueData);
                    setComments(issueData.comments);
                }
            } catch (e) {
                console.error('Failed to load issue', e);
                if (isMounted) {
                    setError('Failed to load issue details.');
                }
            } finally {
                if (isMounted) setLoading(false);
            }
        };

        loadIssue();
        return () => {
            isMounted = false;
        };
    }, [repoId, issueNumber, token]);

    const handleAddComment = async () => {
        if (!repo || !token || !issue) return;
        const text = commentDraft.trim();
        if (!text) return;
        setCommentSaving(true);
        setCommentError('');
        try {
            githubService.initialize(token);
            await githubService.addComment(repo.owner, repo.name, issue.number, text);
            const comments = await githubService.getIssueComments(repo.owner, repo.name, issue.number);
            setComments(comments);
            setIssue(prev => prev ? { ...prev, comments: comments, commentsCount: comments.length } : prev);
            setCommentDraft('');
        } catch (e) {
            console.error('Failed to add comment', e);
            setCommentError('Failed to add comment. Check your token and try again.');
        } finally {
            setCommentSaving(false);
        }
    };

    return (
        <div className="relative flex h-full min-h-screen w-full flex-col overflow-hidden pb-24">
            <header className="sticky top-0 z-20 bg-background-light/95 dark:bg-background-dark/95 backdrop-blur-md border-b border-gray-200 dark:border-white/5 px-4 py-3 flex items-center gap-3">
                <button onClick={() => navigate(-1)} aria-label="Go back" className="p-1 rounded-full hover:bg-black/5 dark:hover:bg-white/10 transition-colors">
                    <span className="material-symbols-outlined text-slate-900 dark:text-white">arrow_back</span>
                </button>
                <div>
                    <h1 className="text-lg font-bold leading-none tracking-tight">Issue Details</h1>
                    <p className="text-xs text-gray-500 dark:text-gray-400 font-medium">{repo?.displayName || repo?.name}</p>
                </div>
            </header>

            <main className="flex-1 px-4 py-4 space-y-4">
                {loading && <div className="text-center py-10"><div className="inline-block size-8 border-2 border-primary border-t-transparent rounded-full animate-spin"></div></div>}
                {!loading && error && (
                    <div className="rounded-lg border border-red-500/30 bg-red-500/10 text-red-200 text-xs px-3 py-2">{error}</div>
                )}
                {!loading && !error && issue && (
                    <>
                        <div className="bg-white dark:bg-surface-dark-lighter rounded-xl p-4 border border-gray-200 dark:border-white/10 space-y-3">
                            <div className="flex items-start justify-between gap-3">
                                <div>
                                    <div className="flex items-center gap-2 mb-1">
                                        <span className="text-xs font-bold uppercase text-gray-500">#{issue.number}</span>
                                        <span className={`text-[10px] font-bold uppercase tracking-wide px-2 py-0.5 rounded ${issue.state === 'open' ? 'bg-primary/10 text-primary' : 'bg-gray-200 text-gray-600'}`}>{issue.state}</span>
                                    </div>
                                    <h2 className="text-lg font-bold text-slate-900 dark:text-white">{issue.title}</h2>
                                    <p className="text-xs text-gray-500 mt-1">Reported by {issue.reporter.name}</p>
                                </div>
                                <span className={`text-xs font-bold uppercase px-2 py-1 rounded ${issue.priority === 'critical' ? 'bg-red-500/10 text-red-500' : issue.priority === 'high' ? 'bg-orange-500/10 text-orange-500' : issue.priority === 'medium' ? 'bg-yellow-500/10 text-yellow-600' : 'bg-green-500/10 text-green-500'}`}>{issue.priority}</span>
                            </div>
                            <div className="bg-gray-50 dark:bg-black/30 rounded-lg p-3">
                                <p className="text-sm text-gray-700 dark:text-gray-200 whitespace-pre-wrap">{issue.description || 'No description provided.'}</p>
                            </div>
                            {issue.labels.length > 0 && (
                                <div className="flex flex-wrap gap-2">
                                    {issue.labels.map(label => (
                                        <span key={label} className="text-[10px] font-semibold uppercase tracking-wide px-2 py-1 rounded bg-gray-100 dark:bg-white/10 text-gray-600 dark:text-gray-300">{label}</span>
                                    ))}
                                </div>
                            )}
                        </div>

                        <div className="bg-white dark:bg-surface-dark-lighter rounded-xl p-4 border border-gray-200 dark:border-white/10 space-y-3">
                            <div className="flex items-center justify-between">
                                <h3 className="text-sm font-bold text-slate-900 dark:text-white">Comments</h3>
                                <span className="text-[11px] text-gray-400">{comments.length} total</span>
                            </div>
                            <div className="space-y-2">
                                <textarea
                                    value={commentDraft}
                                    onChange={(e) => setCommentDraft(e.target.value)}
                                    className="w-full min-h-[120px] rounded-lg bg-gray-50 dark:bg-black/30 p-3 text-sm text-gray-700 dark:text-gray-200"
                                    placeholder="Add a comment..."
                                    maxLength={65536}
                                />
                                {commentError && <div className="rounded-lg border border-red-500/30 bg-red-500/10 px-3 py-2 text-xs text-red-200">{commentError}</div>}
                                <div className="flex justify-end">
                                    <button
                                        onClick={handleAddComment}
                                        disabled={commentSaving || !commentDraft.trim()}
                                        className="rounded-lg bg-primary px-3 py-2 text-xs font-semibold text-black disabled:opacity-50"
                                    >
                                        {commentSaving ? 'Posting...' : 'Post Comment'}
                                    </button>
                                </div>
                            </div>
                            {comments.length === 0 ? (
                                <p className="text-xs text-gray-500">No comments yet.</p>
                            ) : (
                                <div className="space-y-2">
                                    {comments.map(comment => (
                                        <div key={comment.id} className="bg-gray-50 dark:bg-black/30 rounded-lg p-3 text-sm text-gray-700 dark:text-gray-200">
                                            {comment.text}
                                        </div>
                                    ))}
                                </div>
                            )}
                        </div>
                    </>
                )}
            </main>
            <BottomNav onNotesClick={onNotesClick} />
        </div>
    );
};

// 4. Quick Issue Overlay (Unchanged from previous logic, just receiving props)
const QuickIssuePage = ({ repos, globalSettings }: { repos: Repository[]; globalSettings: GlobalSettings }) => {
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();
    const repoId = searchParams.get('repo');
    const buildParam = searchParams.get('build') || '';
    const repo = repos.find(r => r.id === repoId) || repos[0];
    const token = repo ? resolveGithubToken(repo, globalSettings) : '';
    const [title, setTitle] = useState('');
    const [desc, setDesc] = useState('');
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [buildNumber, setBuildNumber] = useState(buildParam || repo?.apps[0]?.buildNumber || '');
    const autoFixAvailable = typeof window !== 'undefined' && Boolean((window as any).QAVT_AUTO_FIX?.issueCreated);
    const autoFixToggleAvailable = typeof window !== 'undefined' && typeof (window as any).QAVT_AUTO_FIX?.setEnabled === 'function';
    const [autoFixEnabled, setAutoFixEnabled] = useState(() => {
        if (typeof window === 'undefined') return false;
        return Boolean((window as any).QAVT_AUTO_FIX?.enabled);
    });

    useEffect(() => {
        setBuildNumber(buildParam || repo?.apps[0]?.buildNumber || '');
    }, [buildParam, repo?.id, repo?.apps]);

    useEffect(() => {
        if (!autoFixAvailable || typeof window === 'undefined') return;
        const handler = (event: Event) => {
            const detail = (event as CustomEvent).detail as { enabled?: boolean } | undefined;
            setAutoFixEnabled(Boolean(detail?.enabled));
        };
        window.addEventListener('qavt-auto-fix-changed', handler);
        setAutoFixEnabled(Boolean((window as any).QAVT_AUTO_FIX?.enabled));
        return () => window.removeEventListener('qavt-auto-fix-changed', handler);
    }, [autoFixAvailable]);

    const handleAutoFixToggle = (nextEnabled: boolean) => {
        setAutoFixEnabled(nextEnabled);
        if (!autoFixToggleAvailable) return;
        try {
            (window as any).QAVT_AUTO_FIX.setEnabled(nextEnabled);
        } catch (notifyError) {
            console.warn('Failed to update auto-fix state', notifyError);
        }
    };

    const handleSubmit = async (e?: React.FormEvent) => {
        if (e) e.preventDefault();
        if (!repo || !token) {
            alert("No GitHub token configured. Add a global or repo token.");
            return;
        }
        setIsSubmitting(true);
        try {
            githubService.initialize(token);
            const created = await githubService.createIssue(repo.id, { title, description: desc, labels: [] }, repo.owner, repo.name);
            const tag = (buildNumber || '').trim();
            if (tag) {
                try {
                    await githubService.addComment(repo.owner, repo.name, created.number, `open v${tag}`);
                } catch (commentError) {
                    console.error('Failed to add build comment', commentError);
                    alert("Issue created, but failed to add the build comment.");
                }
            }
            if (autoFixAvailable && autoFixEnabled) {
                try {
                    const payload = new URLSearchParams({
                        repoId: repo.id,
                        repoOwner: repo.owner,
                        repoName: repo.name,
                        issueNumber: created.number.toString(),
                        title: created.title || title,
                        description: desc || '',
                        buildNumber: tag
                    }).toString();
                    (window as any).QAVT_AUTO_FIX.issueCreated(payload);
                } catch (notifyError) {
                    console.warn('Failed to notify auto-fix bridge', notifyError);
                }
            }
            setTitle('');
            setDesc('');
            alert("Issue created successfully!");
        } catch (e) {
            alert("Failed to create issue.");
        } finally {
            setIsSubmitting(false);
        }
    };
    // ... Simplified render for brevity, assuming standard overlay UI ...
     return (
        <div className="bg-transparent h-screen w-full relative">
            <div onClick={() => navigate(-1)} className="absolute inset-0 bg-black/60 backdrop-blur-sm z-10"></div>
            <div className="absolute bottom-0 left-0 right-0 z-20 flex flex-col h-[90vh] bg-surface-dark rounded-t-[32px] shadow-2xl border-t border-white/10 animate-slide-up">
                 <div className="sticky top-0 z-30 bg-surface-dark border-b border-white/10">
                      <div className="w-full flex justify-center pt-4 pb-2"><div className="w-14 h-1.5 bg-gray-600/40 rounded-full"></div></div>
                      <div className="px-6 py-2 flex justify-between items-center">
                          <div>
                              <h2 className="text-white text-2xl font-bold">New Issue</h2>
                              {autoFixAvailable && (
                                  <div className="mt-2 flex items-center gap-2 text-[11px] text-gray-400">
                                      <span>Auto-fix on submit</span>
                                      <button
                                          type="button"
                                          role="switch"
                                          aria-checked={autoFixEnabled}
                                          onClick={() => handleAutoFixToggle(!autoFixEnabled)}
                                          className={`relative inline-flex h-5 w-9 items-center rounded-full transition ${
                                              autoFixEnabled ? 'bg-primary/90' : 'bg-white/15'
                                          }`}
                                      >
                                          <span
                                              className={`inline-block h-4 w-4 transform rounded-full bg-white transition ${
                                                  autoFixEnabled ? 'translate-x-4' : 'translate-x-1'
                                              }`}
                                          />
                                      </button>
                                      <span>{autoFixEnabled ? 'On' : 'Off'}</span>
                                  </div>
                              )}
                          </div>
                          <button onClick={() => navigate(-1)} aria-label="Close" className="size-10 rounded-full bg-white/5 flex items-center justify-center text-white">
                              <span className="material-symbols-outlined">close</span>
                          </button>
                      </div>
                 </div>
                 <div className="flex-1 overflow-y-auto scroll-smooth">
                 <form onSubmit={handleSubmit} className="p-6 space-y-4">
                      <div className="space-y-1">
                          <label htmlFor="quick-issue-build" className="text-xs uppercase font-bold text-gray-500">Found in Build</label>
                          <input
                              id="quick-issue-build"
                              value={buildNumber}
                              onChange={(e) => setBuildNumber(e.target.value)}
                              className="w-full bg-input-dark rounded-xl px-4 py-3 text-white font-mono text-sm focus:ring-primary focus:border-primary"
                              placeholder="e.g. 10"
                              maxLength={20}
                          />
                      </div>
                      <div className="space-y-1">
                          <label htmlFor="quick-issue-title" className="text-xs uppercase font-bold text-gray-500">Title</label>
                          {/* SECURITY: Enforce length limit (GitHub issue title max 256 chars) */}
                          <input id="quick-issue-title" value={title} onChange={e=>setTitle(e.target.value)} maxLength={256} className="w-full bg-input-dark rounded-xl px-4 py-4 text-white text-lg focus:ring-primary focus:border-primary" placeholder="Title" autoFocus required />
                      </div>
                      <div className="space-y-1">
                          <label htmlFor="quick-issue-desc" className="text-xs uppercase font-bold text-gray-500">Description</label>
                          {/* SECURITY: Enforce length limit (GitHub issue body max 65536 chars) */}
                          <textarea id="quick-issue-desc" value={desc} onChange={e=>setDesc(e.target.value)} maxLength={65536} className="w-full bg-input-dark rounded-xl p-4 text-white min-h-[200px] focus:ring-primary focus:border-primary" placeholder="Description"></textarea>
                      </div>
                      <button type="submit" disabled={!title || isSubmitting} className="w-full bg-primary h-14 rounded-xl font-bold text-black disabled:opacity-50">{isSubmitting ? 'Submitting...' : 'Submit'}</button>
                 </form>
                 </div>
            </div>
        </div>
    );
};

// ... ConflictPage remains as a UI stub ...
const ConflictPage = () => { const navigate = useNavigate(); return <div className="h-screen bg-background-dark text-white p-6"><button onClick={()=>navigate(-1)} className="mb-4 text-primary">Back</button><h1>Conflicts</h1><p>Manual resolution required.</p></div> };

const RequireAuth = ({ user, children }: { user: User | null; children: React.ReactElement }) => {
  if (!user) {
    return <Navigate to="/login" replace />;
  }
  return children;
};

const App = () => {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);
  const [repos, setRepos] = useState<Repository[]>([]);
  const [globalSettings, setGlobalSettings] = useState<GlobalSettings>({});
  const [notesOpen, setNotesOpen] = useState(false);

  useEffect(() => {
    const unsubscribe = onAuthStateChanged(auth, (currentUser) => {
        setUser(currentUser);
        setLoading(false);
    });
    return () => unsubscribe();
  }, []);

  useEffect(() => {
    // Initialize Theme
    themeService.applyTheme(themeService.getSavedThemeId());
    themeService.setDarkMode(themeService.getSavedMode());
  }, []);

  useEffect(() => {
    if (user) {
        const unsubscribe = firebaseService.subscribeToRepos(user.uid, (data) => {
            setRepos(normalizeRepos(data));
        });
        const unsubscribeGlobal = firebaseService.subscribeToGlobalSettings(user.uid, (data) => {
            setGlobalSettings(data || {});
        });
        return () => {
            unsubscribe();
            unsubscribeGlobal();
        };
    }
  }, [user]);

  if (loading) return <div className="min-h-screen bg-background-dark flex items-center justify-center"><div className="size-10 border-4 border-primary border-t-transparent rounded-full animate-spin"></div></div>;

  return (
    <HashRouter>
      <Routes>
        <Route path="/login" element={user ? <Navigate to="/" replace /> : <LoginPage />} />
        <Route
          path="/"
          element={
            <RequireAuth user={user}>
              <HomePage repos={repos} user={user!} globalSettings={globalSettings} onNotesClick={() => setNotesOpen(true)} />
            </RequireAuth>
          }
        />
        <Route
          path="/dashboard"
          element={
            <RequireAuth user={user}>
              <Dashboard repos={repos} user={user!} globalSettings={globalSettings} onNotesClick={() => setNotesOpen(true)} />
            </RequireAuth>
          }
        />
        <Route
          path="/issue/:repoId/:issueNumber"
          element={
            <RequireAuth user={user}>
              <IssueDetailPage repos={repos} globalSettings={globalSettings} onNotesClick={() => setNotesOpen(true)} />
            </RequireAuth>
          }
        />
        <Route
          path="/config"
          element={
            <RequireAuth user={user}>
              <ConfigurationPage repos={repos} setRepos={setRepos} user={user!} globalSettings={globalSettings} setGlobalSettings={setGlobalSettings} onNotesClick={() => setNotesOpen(true)} />
            </RequireAuth>
          }
        />
        <Route
          path="/quick-issue"
          element={
            <RequireAuth user={user}>
              <QuickIssuePage repos={repos} globalSettings={globalSettings} />
            </RequireAuth>
          }
        />
        <Route
          path="/conflicts"
          element={
            <RequireAuth user={user}>
              <ConflictPage />
            </RequireAuth>
          }
        />
        <Route path="*" element={<Navigate to={user ? "/" : "/login"} replace />} />
      </Routes>
      {user && <Notes isOpen={notesOpen} onClose={() => setNotesOpen(false)} userId={user.uid} />}
    </HashRouter>
  );
};

export default App;

