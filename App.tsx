import React, { useState, useEffect } from 'react';
import { HashRouter, Routes, Route, Link, useNavigate, useLocation, useSearchParams, Navigate } from 'react-router-dom';
import { Repository, AppConfig, Issue, PullRequest } from './types';
import { githubService } from './services/githubService';
import { auth, firebaseService } from './services/firebase';
import { aiService } from './services/aiService';
import { onAuthStateChanged, signInWithEmailAndPassword, createUserWithEmailAndPassword, signOut, User } from 'firebase/auth';

// --- Shared UI Components ---

const BottomNav = () => (
  <nav className="fixed bottom-0 left-0 w-full bg-background-light/80 dark:bg-background-dark/90 backdrop-blur-lg border-t border-gray-200 dark:border-white/10 z-50 pb-safe">
    <div className="flex items-center justify-around h-16 max-w-md mx-auto">
        <Link to="/" className="flex flex-col items-center justify-center w-16 h-full gap-1 text-primary group">
            <span className="material-symbols-outlined text-[24px]">grid_view</span>
            <span className="text-[10px] font-bold">Projects</span>
        </Link>
        <Link to="/config" className="flex flex-col items-center justify-center w-16 h-full gap-1 text-gray-500 dark:text-gray-400 hover:text-gray-900 dark:hover:text-white transition-colors">
            <span className="material-symbols-outlined text-[24px]">settings</span>
            <span className="text-[10px] font-medium">Config</span>
        </Link>
    </div>
  </nav>
);

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
                    <div className="inline-flex items-center justify-center size-16 rounded-2xl bg-primary/20 text-primary mb-4">
                        <span className="material-symbols-outlined text-4xl">bug_report</span>
                    </div>
                    <h1 className="text-3xl font-bold text-white tracking-tight">QA Verify & Track</h1>
                    <p className="text-gray-400">Sync your testing workflow across all devices.</p>
                </div>

                <div className="bg-surface-dark p-6 rounded-2xl border border-white/5 space-y-6">
                    {error && <div className="p-3 bg-red-500/10 border border-red-500/20 text-red-400 text-sm rounded-lg">{error}</div>}
                    
                    <form onSubmit={handleAuth} className="space-y-4">
                        <div className="space-y-1">
                            <label htmlFor="email" className="text-xs uppercase font-bold text-gray-500">Email</label>
                            <input id="email" value={email} onChange={e=>setEmail(e.target.value)} type="email" required className="w-full bg-input-dark border-transparent rounded-lg p-3 text-white focus:ring-primary focus:border-primary" placeholder="qa@acme.inc" />
                        </div>
                        <div className="space-y-1">
                            <label htmlFor="password" className="text-xs uppercase font-bold text-gray-500">Password</label>
                            <input id="password" value={password} onChange={e=>setPassword(e.target.value)} type="password" required className="w-full bg-input-dark border-transparent rounded-lg p-3 text-white focus:ring-primary focus:border-primary" placeholder="••••••••" />
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
const HomePage = ({ repos, user }: { repos: Repository[], user: User }) => {
    return (
        <div className="relative flex h-full min-h-screen w-full flex-col overflow-hidden pb-20">
            <header className="sticky top-0 z-10 flex items-center justify-between bg-background-light/95 dark:bg-background-dark/95 backdrop-blur-md p-4 pb-2 border-b border-gray-200 dark:border-white/5">
                <div className="flex items-center gap-3">
                    <div className="relative group cursor-pointer" onClick={() => signOut(auth)}>
                        <img src={user.photoURL || `https://ui-avatars.com/api/?name=${user.email}`} className="size-10 rounded-full border-2 border-transparent hover:border-primary transition-colors bg-surface-dark" alt="Profile" />
                        <div className="absolute bottom-0 right-0 size-3 bg-primary rounded-full border-2 border-background-light dark:border-background-dark"></div>
                    </div>
                    <div>
                        <h2 className="text-xl font-bold leading-tight tracking-tight">My Projects</h2>
                        <p className="text-xs text-gray-500 dark:text-gray-400 font-medium truncate max-w-[150px]">{user.email}</p>
                    </div>
                </div>
                <Link to="/config" aria-label="Add new repository" className="flex items-center justify-center size-10 rounded-full bg-white/5 hover:bg-white/10 text-primary border border-primary/20 transition-all">
                    <span className="material-symbols-outlined font-bold">add_link</span>
                </Link>
            </header>

            <main className="flex-1 flex flex-col gap-6 p-4">
                {repos.length === 0 ? (
                    <div className="text-center py-20 opacity-60">
                         <span className="material-symbols-outlined text-6xl mb-4">move_to_inbox</span>
                         <p className="text-lg font-bold">No Projects Found</p>
                         <p className="text-sm">Go to Config to add your first repo.</p>
                    </div>
                ) : (
                    repos.map(repo => (
                        <div key={repo.id} className="group flex flex-col gap-0 rounded-xl bg-white dark:bg-[#1a2e1c] shadow-md hover:shadow-lg transition-shadow overflow-hidden border border-gray-100 dark:border-white/5">
                            <div className="p-4 flex items-start justify-between">
                                <div className="flex flex-col gap-1">
                                    <div className="flex items-center gap-2 text-gray-400 mb-1">
                                        <span className="material-symbols-outlined text-sm">folder_open</span>
                                        <span className="text-xs font-medium uppercase tracking-wider">{repo.apps[0]?.platform || 'Generic'}</span>
                                    </div>
                                    <h3 className="text-lg font-bold leading-tight group-hover:text-primary transition-colors">
                                        {repo.displayName || repo.name}
                                        <span className="text-xs font-normal text-gray-500 ml-2">({repo.owner}/{repo.name})</span>
                                    </h3>
                                    <div className="flex items-center gap-2 mt-1">
                                        <span className="relative flex h-2 w-2">
                                            <span className="relative inline-flex rounded-full h-2 w-2 bg-primary"></span>
                                        </span>
                                        <p className="text-primary text-xs font-bold">Build #{repo.apps[0]?.buildNumber}</p>
                                    </div>
                                </div>
                                <Link to={`/dashboard?repo=${repo.id}`} aria-label="View repository dashboard" className="flex items-center justify-center size-10 rounded-full bg-gray-100 dark:bg-[#253827] text-primary">
                                    <span className="material-symbols-outlined">arrow_forward</span>
                                </Link>
                            </div>
                        </div>
                    ))
                )}
            </main>
            <BottomNav />
        </div>
    );
};

// 2. Configuration Page
const ConfigurationPage = ({ repos, setRepos, user }: { repos: Repository[], setRepos: React.Dispatch<React.SetStateAction<Repository[]>>, user: User }) => {
    const navigate = useNavigate();
    const [view, setView] = useState<'list' | 'edit'>('list');
    const [activeRepoId, setActiveRepoId] = useState<string | null>(null);
    const [saving, setSaving] = useState(false);
    const [saveError, setSaveError] = useState('');

    // Form State
    const [formData, setFormData] = useState<Partial<Repository>>({
        name: '',
        owner: '',
        displayName: '',
        githubToken: '',
        apps: []
    });

    const startEdit = (repo?: Repository) => {
        if (repo) {
            setActiveRepoId(repo.id);
            setFormData(repo);
        } else {
            setActiveRepoId(null);
            setFormData({
                id: Date.now().toString(),
                name: '',
                owner: '',
                displayName: '',
                githubToken: '',
                apps: [{ id: Date.now().toString(), name: 'New App', platform: 'android', buildNumber: '1' }],
                projects: [],
                templates: []
            });
        }
        setView('edit');
    };

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

    if (view === 'list') {
        return (
            <div className="bg-background-light dark:bg-background-dark min-h-screen pb-safe">
                <header className="sticky top-0 z-20 flex items-center justify-between p-4 bg-background-light/95 dark:bg-background-dark/95 backdrop-blur-md border-b border-gray-200 dark:border-white/5">
                    <div className="flex items-center gap-3">
                        <button onClick={() => navigate(-1)} aria-label="Go back" className="p-1 rounded-full hover:bg-black/5 dark:hover:bg-white/10">
                            <span className="material-symbols-outlined text-slate-900 dark:text-white">arrow_back</span>
                        </button>
                        <h1 className="text-lg font-bold">Configuration</h1>
                    </div>
                    <button onClick={() => startEdit()} className="flex items-center gap-1 bg-primary text-black px-3 py-1.5 rounded-lg text-sm font-bold">
                        <span className="material-symbols-outlined text-lg">add</span> New
                    </button>
                </header>
                <main className="p-4 space-y-4">
                    <h2 className="text-xs uppercase text-gray-500 font-bold tracking-wider">Repositories</h2>
                    {repos.map(repo => (
                        <div key={repo.id} onClick={() => startEdit(repo)} className="p-4 bg-white dark:bg-surface-dark rounded-xl border border-gray-200 dark:border-white/5 flex justify-between items-center cursor-pointer active:scale-[0.99] transition-transform">
                            <div>
                                <h3 className="font-bold text-slate-900 dark:text-white">{repo.displayName || repo.name}</h3>
                                <p className="text-xs text-gray-500">{repo.owner}/{repo.name}</p>
                            </div>
                            <span className="material-symbols-outlined text-gray-400">edit</span>
                        </div>
                    ))}
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
                            <label className="text-xs text-gray-500">Display Name</label>
                            <input value={formData.displayName} onChange={e => setFormData({...formData, displayName: e.target.value})} type="text" className="w-full bg-white dark:bg-input-dark border-gray-200 dark:border-white/10 rounded-lg px-4 py-3 text-sm focus:ring-primary focus:border-primary text-slate-900 dark:text-white" />
                        </div>
                        <div className="grid grid-cols-2 gap-3">
                            <div className="space-y-1">
                                <label className="text-xs text-gray-500">Owner</label>
                                <input value={formData.owner} onChange={e => setFormData({...formData, owner: e.target.value})} type="text" placeholder="org" className="w-full bg-white dark:bg-input-dark border-gray-200 dark:border-white/10 rounded-lg px-4 py-3 text-sm focus:ring-primary focus:border-primary text-slate-900 dark:text-white" />
                            </div>
                            <div className="space-y-1">
                                <label className="text-xs text-gray-500">Repo Name</label>
                                <input value={formData.name} onChange={e => setFormData({...formData, name: e.target.value})} type="text" placeholder="repo" className="w-full bg-white dark:bg-input-dark border-gray-200 dark:border-white/10 rounded-lg px-4 py-3 text-sm focus:ring-primary focus:border-primary text-slate-900 dark:text-white" />
                            </div>
                        </div>
                        <div className="space-y-1">
                            <label className="text-xs text-gray-500">GitHub Personal Access Token (PAT)</label>
                            <input value={formData.githubToken || ''} onChange={e => setFormData({...formData, githubToken: e.target.value})} type="password" placeholder="ghp_..." className="w-full bg-white dark:bg-input-dark border-gray-200 dark:border-white/10 rounded-lg px-4 py-3 text-sm focus:ring-primary focus:border-primary text-slate-900 dark:text-white" />
                            <p className="text-[10px] text-gray-500">Required for reading/writing to this private/public repo.</p>
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
                                    <label className="text-[10px] uppercase text-gray-500">App Name</label>
                                    <input value={app.name} onChange={e => updateApp(idx, 'name', e.target.value)} type="text" className="w-full bg-background-light dark:bg-background-dark border-transparent rounded px-2 py-1.5 text-sm text-slate-900 dark:text-white" />
                                </div>
                                <div className="space-y-1">
                                    <label className="text-[10px] uppercase text-gray-500">Platform</label>
                                    <select value={app.platform} onChange={e => updateApp(idx, 'platform', e.target.value as any)} className="w-full bg-background-light dark:bg-background-dark border-transparent rounded px-2 py-1.5 text-sm text-slate-900 dark:text-white">
                                        <option value="android">Android</option>
                                        <option value="ios">iOS</option>
                                        <option value="web">Web</option>
                                    </select>
                                </div>
                            </div>
                            <div className="space-y-1">
                                <label className="text-[10px] uppercase text-gray-500">Test URL (Play Store/TestFlight)</label>
                                <input value={app.playStoreUrl || ''} onChange={e => updateApp(idx, 'playStoreUrl', e.target.value)} type="text" placeholder="https://..." className="w-full bg-background-light dark:bg-background-dark border-transparent rounded px-2 py-1.5 text-sm text-slate-900 dark:text-white" />
                            </div>
                            <div className="space-y-1">
                                <label className="text-[10px] uppercase text-gray-500">Current Build #</label>
                                <input value={app.buildNumber} onChange={e => updateApp(idx, 'buildNumber', e.target.value)} type="text" className="w-24 bg-background-light dark:bg-background-dark border-transparent rounded px-2 py-1.5 text-sm font-mono font-bold text-slate-900 dark:text-white" />
                            </div>
                        </div>
                    ))}
                </section>
            </main>
        </div>
    );
};

// 3. Dashboard
const Dashboard = ({ repos }: { repos: Repository[] }) => {
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();
    const repoId = searchParams.get('repo');
    const repo = repos.find(r => r.id === repoId) || repos[0];

    const [tab, setTab] = useState<'issues' | 'prs'>('issues');
    const [buildNumber, setBuildNumber] = useState(repo?.apps[0]?.buildNumber || '0');

    // Ensure GitHub client is ready whenever the selected repo/token changes
    useEffect(() => {
        if (repo?.githubToken) {
            githubService.initialize(repo.githubToken);
        }
    }, [repo?.githubToken]);

    // Real Data State
    const [issues, setIssues] = useState<Issue[]>([]);
    const [prs, setPrs] = useState<PullRequest[]>([]);
    const [loading, setLoading] = useState(false);
    
    // State for PR actions
    const [prProcessing, setPrProcessing] = useState<number | null>(null);
    const [undoPr, setUndoPr] = useState<{id: number, pr: PullRequest} | null>(null);
    const [manualResolveIds, setManualResolveIds] = useState<Set<number>>(new Set());

    // AI Analysis State
  const [analyzingIds, setAnalyzingIds] = useState<Set<number>>(new Set());
  const [analysisResults, setAnalysisResults] = useState<Record<number, string>>({});
  const [syncError, setSyncError] = useState<string>('');
  const [prError, setPrError] = useState<string>('');

    // Initial Fetch
    useEffect(() => {
        if (repo && repo.githubToken) {
            handleSync(false); // Initial load without auto-populating build number
        }
    }, [repo]);

    // fetchStoreBuild: when true, try to auto-populate build number from stored app config (no external store fetch to avoid CORS)
    const handleSync = async (fetchStoreBuild: boolean = false) => {
        setLoading(true);
        setSyncError('');
        if (fetchStoreBuild && repo?.apps[0]?.playStoreUrl) {
             const storedBuild = repo.apps[0]?.buildNumber;
             if (storedBuild) {
                 setBuildNumber(storedBuild.toString());
             }
        }

        try {
            const [fetchedIssues, fetchedPrs] = await Promise.all([
                githubService.getIssues(repo.owner, repo.name),
                githubService.getPullRequests(repo.owner, repo.name)
            ]);

            // Filter out issues that already have status comments for this build number
            const statusRegex = /\b(open|closed|blocked)\s*v?\s*(\d+)\b/gi;
            const issuesWithComments = await Promise.all(
              fetchedIssues.map(async (issue) => {
                try {
                  const comments = await githubService.getIssueComments(repo.owner, repo.name, issue.number);
                  const targetBuild = parseInt((buildNumber || '0').trim(), 10);

                  let matched = false;
                  let maxBuild = -1;
                  for (const c of comments) {
                    const body = c.body || '';
                    let m;
                    statusRegex.lastIndex = 0;
                    while ((m = statusRegex.exec(body)) !== null) {
                      matched = true;
                      const b = parseInt(m[2], 10);
                      if (!isNaN(b)) {
                        maxBuild = Math.max(maxBuild, b);
                      }
                    }
                  }

                  // Hide if any status comment references this build or a higher one
                  if (matched && maxBuild >= targetBuild) {
                    return null;
                  }
                  return issue;
                } catch (err) {
                  // If comments fail to load, keep the issue visible
                  console.error('Failed to load comments for issue', issue.number, err);
                  return issue;
                }
              })
            );

            setIssues(issuesWithComments.filter(Boolean) as Issue[]);
            setPrs(fetchedPrs);
        } catch (error) {
            console.error("Sync failed", error);
            setSyncError('Failed to sync GitHub data. Check your token and network.');
        } finally {
            setLoading(false);
        }
    };

    const handleFixed = async (id: number, number: number) => {
        setIssues(issues.filter(i => i.id !== id));
        await githubService.addComment(repo.owner, repo.name, number, `verified fixed ${buildNumber}`);
        await githubService.updateIssueStatus(repo.owner, repo.name, number, 'closed');
    };

    const handleOpen = async (id: number, number: number) => {
        setIssues(issues.filter(i => i.id !== id));
        await githubService.addComment(repo.owner, repo.name, number, `open ${buildNumber}`);
        // Ensure it stays open (no-op mostly, but good for tracking)
    };

    // --- AI Analysis Handler ---
    const handleAnalyze = async (issue: Issue) => {
        if (analyzingIds.has(issue.id)) return;
        
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
    };

    const handleMergeSequence = async (pr: PullRequest) => {
        setPrProcessing(pr.id);
        setPrError('');
        try {
            if (pr.isDraft) await githubService.updatePR(repo.owner, repo.name, pr.number, { isDraft: false });
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

    const handleResolveConflicts = async (pr: PullRequest) => {
        setPrProcessing(pr.id);
        setPrError('');

        try {
            // 1. Fetch details to confirm conflict
            const details = await githubService.getPullRequest(repo.owner, repo.name, pr.number);
            
            // mergeable state: true (clean), false (conflict), null (unknown/computing)
            if (details.mergeable === true) {
                 // It's actually fine, update UI
                 setPrs(prev => prev.map(p => p.id === pr.id ? { ...p, hasConflicts: false } : p));
                 setPrProcessing(null);
                 return;
            }
    
            // If mergeable is false (conflicts) or null (we might try anyway or wait, let's try update)
            // Attempt auto-resolve (update branch)
            const success = await githubService.updateBranch(repo.owner, repo.name, pr.number);
            
            if (success) {
                // Updated branch, GitHub will recompute mergeability.
                // Let's hide it from list implying it's being handled/rebuilding
                 setPrs(prev => prev.filter(p => p.id !== pr.id));
            } else {
                // Failed, likely complex conflict
                 setManualResolveIds(prev => new Set(prev).add(pr.id));
                 setPrError('Auto-resolve failed. Open the conflict view in GitHub.');
            }
        } catch (e) {
            console.error("Resolve failed", e);
            setManualResolveIds(prev => new Set(prev).add(pr.id));
            setPrError('Resolve failed. Check permissions or try manually in GitHub.');
        } finally {
            setPrProcessing(null);
        }
    };

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
    if (!repo.githubToken) return <div className="p-10 text-center">Missing GitHub Token for this repo. Please configure it.</div>;

    return (
        <div className="relative flex h-full min-h-screen w-full flex-col overflow-x-hidden pb-24">
            <header className="sticky top-0 z-20 bg-background-light/95 dark:bg-background-dark/95 backdrop-blur-md border-b border-gray-200 dark:border-white/5 px-4 py-3 flex items-center justify-between">
                <div className="flex items-center gap-3">
                    <button onClick={() => navigate('/')} aria-label="Go back to home" className="p-1 rounded-full hover:bg-black/5 dark:hover:bg-white/10 transition-colors">
                        <span className="material-symbols-outlined text-slate-900 dark:text-white">arrow_back</span>
                    </button>
                    <div>
                        <h1 className="text-lg font-bold leading-none tracking-tight">QA Dashboard</h1>
                        <p className="text-xs text-gray-500 dark:text-gray-400 font-medium">{repo?.displayName || repo?.name}</p>
                    </div>
                </div>
                <div className="flex items-center bg-gray-200 dark:bg-surface-dark-lighter rounded-lg p-0.5 border border-transparent dark:border-white/10">
                    <button onClick={() => setTab('issues')} className={`px-3 py-1.5 rounded-md text-xs font-bold transition-all ${tab === 'issues' ? 'bg-primary text-black shadow-sm' : 'text-gray-500 dark:text-gray-400 hover:text-slate-900 dark:hover:text-white'}`}>Issues</button>
                    <button onClick={() => setTab('prs')} className={`px-3 py-1.5 rounded-md text-xs font-bold transition-all ${tab === 'prs' ? 'bg-primary text-black shadow-sm' : 'text-gray-500 dark:text-gray-400 hover:text-slate-900 dark:hover:text-white'}`}>PRs</button>
                </div>
            </header>

            <section className="px-4 py-5 space-y-3">
                 <div className="flex items-end gap-3">
                    <div className="flex-1 space-y-1.5">
                        <label className="text-xs font-semibold text-gray-500 dark:text-[#9db99f] uppercase tracking-wider">Target Build</label>
                        <div className="relative flex items-center gap-2">
                            <span className="absolute left-3 material-symbols-outlined text-gray-400 text-[20px]">tag</span>
                            <input className="w-full bg-white dark:bg-[#1c2e1f] border-gray-200 dark:border-white/5 rounded-lg py-3 pl-10 pr-3 font-mono font-bold text-lg focus:ring-2 focus:ring-primary focus:border-primary transition-all text-slate-900 dark:text-white" type="text" value={buildNumber} onChange={(e) => setBuildNumber(e.target.value)}/>
                            <div className="flex items-center gap-2">
                                <button
                                    onClick={() => handleSync(false)}
                                    aria-label="Sync with GitHub"
                                    className="bg-white dark:bg-[#253827] border border-gray-200 dark:border-white/5 rounded-lg px-3 py-3 hover:text-primary transition-colors flex items-center gap-1"
                                >
                                    <span className="material-symbols-outlined">sync</span>
                                    <span className="text-xs font-bold hidden sm:inline">Sync</span>
                                </button>
                                <button
                                    onClick={() => handleSync(true)}
                                    aria-label="Sync with Store"
                                    className="bg-white dark:bg-[#253827] border border-gray-200 dark:border-white/5 rounded-lg px-3 py-3 hover:text-primary transition-colors flex items-center gap-1"
                                >
                                    <span className="material-symbols-outlined">system_update_alt</span>
                                    <span className="text-xs font-bold hidden sm:inline">Store</span>
                                </button>
                            </div>
                        </div>
                    </div>
                    <div className="flex-1 space-y-1.5">
                         <label className="text-xs font-semibold text-gray-500 dark:text-[#9db99f] uppercase tracking-wider">App Variant</label>
                         <select className="w-full bg-white dark:bg-[#1c2e1f] border-gray-200 dark:border-white/5 rounded-lg h-[54px] px-3 font-medium text-sm text-slate-900 dark:text-white focus:ring-primary focus:border-primary">
                             {repo?.apps.map(app => <option key={app.id} value={app.id}>{app.name} ({app.platform})</option>)}
                         </select>
                    </div>
                 </div>
            </section>
            <div className="h-px w-full bg-gray-200 dark:bg-white/5 mb-4"></div>

            <main className="flex-1 px-4 space-y-4">
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
                        <div className="flex justify-between items-center pb-1">
                             <h2 className="text-sm font-bold text-slate-900 dark:text-white">Pending Verification</h2>
                             <span className="text-[11px] text-gray-400">{issues.length} remaining</span>
                        </div>
                        {issues.length === 0 ? (
                            <div className="text-center py-10 opacity-50"><span className="material-symbols-outlined text-6xl text-gray-600">check_circle</span><p className="mt-4 text-gray-400">All cleared for build {buildNumber}!</p></div>
                        ) : (
                            issues.map(issue => (
                                <article key={issue.id} className="relative flex flex-col gap-2 rounded-lg bg-white dark:bg-surface-dark-lighter/80 p-3 shadow-sm border border-gray-100 dark:border-white/10 animate-fade-in">
                                    <div className="flex items-start justify-between gap-2.5">
                                        <div className="flex flex-col gap-1">
                                            <div className="flex items-center gap-2">
                                                <span className={`font-bold text-xs tracking-tight flex items-center gap-1 uppercase ${issue.priority === 'critical' ? 'text-red-500' : issue.priority === 'high' ? 'text-orange-500' : 'text-blue-400'}`}>
                                                    <span className="material-symbols-outlined text-[14px] fill-1">{issue.priority === 'critical' ? 'error' : 'flag'}</span>{issue.priority}
                                                </span>
                                                <span className="text-gray-500 dark:text-gray-500 text-[11px] font-medium">#{issue.number}</span>
                                            </div>
                                            <a href={`https://github.com/${repo.owner}/${repo.name}/issues/${issue.number}`} target="_blank" rel="noopener noreferrer" className="text-base font-semibold text-slate-900 dark:text-white leading-snug hover:text-primary">
                                                {issue.title}
                                            </a>
                                        </div>
                                    </div>
                                    <div className="bg-gray-50 dark:bg-black/30 rounded-lg p-2">
                                        <p className="text-xs text-gray-600 dark:text-gray-200 line-clamp-3 font-mono leading-snug">{issue.description}</p>
                                    </div>

                                    {/* AI Analysis Result Display */}
                                    {analysisResults[issue.id] && (
                                        <div className="bg-primary/10 border border-primary/25 rounded-lg p-2.5 animate-fade-in">
                                            <div className="flex items-center gap-1 mb-1 text-primary">
                                                <span className="material-symbols-outlined text-[15px]">smart_toy</span>
                                                <span className="text-[11px] font-bold uppercase">Gemini Analysis</span>
                                            </div>
                                            <p className="text-[11px] text-slate-800 dark:text-gray-100 whitespace-pre-wrap leading-relaxed">{analysisResults[issue.id]}</p>
                                        </div>
                                    )}

                                    <div className="grid grid-cols-4 gap-2 mt-1">
                                        <button onClick={() => handleFixed(issue.id, issue.number)} className="col-span-1 flex flex-col items-center justify-center gap-1 h-11 rounded-lg bg-primary text-black font-semibold text-[11px] shadow-[0_2px_8px_rgba(19,236,37,0.25)] active:scale-95 transition-transform"><span className="material-symbols-outlined text-[18px]">check_circle</span>Fixed</button>
                                        <button onClick={() => handleOpen(issue.id, issue.number)} className="col-span-1 flex flex-col items-center justify-center gap-1 h-11 rounded-lg bg-orange-500 text-white font-semibold text-[11px] shadow-[0_2px_8px_rgba(249,115,22,0.25)] active:scale-95 transition-transform"><span className="material-symbols-outlined text-[18px]">warning</span>Open</button>
                                        <Link to={`/block/${issue.id}`} className="col-span-1 flex flex-col items-center justify-center gap-1 h-11 rounded-lg bg-red-600 text-white font-semibold text-[11px] shadow-[0_2px_8px_rgba(220,38,38,0.25)] active:scale-95 transition-transform"><span className="material-symbols-outlined text-[18px]">block</span>Blocked</Link>
                                        <button 
                                            onClick={() => handleAnalyze(issue)} 
                                            disabled={analyzingIds.has(issue.id)}
                                            className="col-span-1 flex flex-col items-center justify-center gap-1 h-11 rounded-lg bg-blue-600/10 dark:bg-blue-500/10 text-blue-600 dark:text-blue-300 border border-blue-600/20 dark:border-blue-500/25 font-semibold text-[11px] active:scale-95 transition-transform disabled:opacity-50"
                                        >
                                            {analyzingIds.has(issue.id) ? (
                                                <span className="size-4 border-2 border-current border-t-transparent rounded-full animate-spin"></span>
                                            ) : (
                                                <span className="material-symbols-outlined text-[18px]">analytics</span>
                                            )}
                                            Analyze
                                        </button>
                                    </div>
                                </article>
                            ))
                        )}
                    </>
                )}

                {!loading && tab === 'prs' && (
                    <div className="space-y-4">
                        {prError && <div className="rounded-lg border border-red-500/30 bg-red-500/10 text-red-200 text-xs px-3 py-2">{prError}</div>}
                        {prs.map(pr => {
                             const isProcessing = prProcessing === pr.id;
                             const isManual = manualResolveIds.has(pr.id);

                             return (
                                <div key={pr.id} className="group relative bg-white dark:bg-[#1A1616] rounded-xl p-4 border border-gray-200 dark:border-white/10 shadow-sm transition-all animate-fade-in">
                                    <div className="flex justify-between items-start mb-2">
                                        <div className="pr-2">
                                            <div className="flex items-center gap-2 mb-1">
                                                {pr.isDraft && <span className="px-1.5 py-0.5 rounded text-[10px] font-bold bg-gray-200 dark:bg-gray-700 text-gray-600 dark:text-gray-300 uppercase tracking-wide">Draft</span>}
                                                {pr.hasConflicts && <span className="px-1.5 py-0.5 rounded text-[10px] font-bold bg-red-500/10 text-red-500 uppercase tracking-wide flex items-center gap-1"><span className="material-symbols-outlined text-[12px]">warning</span>Conflict</span>}
                                            </div>
                                            <h3 className="text-slate-900 dark:text-white font-bold text-sm leading-tight">{pr.title}</h3>
                                            <div className="text-xs text-gray-500 mt-1">#{pr.number} • {pr.author.name}</div>
                                        </div>
                                    </div>
                                    <div className="flex items-center gap-2 text-xs text-gray-500 mb-4 font-mono bg-gray-50 dark:bg-white/5 p-2 rounded-lg border border-gray-200 dark:border-white/5">
                                        <span className="text-slate-700 dark:text-gray-300">{pr.branch}</span><span className="material-symbols-outlined text-[12px]">arrow_forward</span><span className="text-slate-700 dark:text-gray-300">{pr.targetBranch}</span>
                                    </div>
                                    
                                    <div className="flex gap-2">
                                        {pr.hasConflicts ? (
                                            isManual ? (
                                                <a 
                                                    href={`https://github.com/${repo.owner}/${repo.name}/pull/${pr.number}/conflicts`}
                                                    target="_blank"
                                                    rel="noopener noreferrer"
                                                    className="flex-1 bg-amber-600/20 text-amber-700 dark:text-amber-500 border border-amber-600/30 font-semibold text-xs py-2.5 rounded-lg flex items-center justify-center gap-1.5 active:scale-[0.98] hover:bg-amber-600/30"
                                                >
                                                    <span className="material-symbols-outlined text-[16px]">open_in_new</span>
                                                    Open in GitHub
                                                </a>
                                            ) : (
                                                <button 
                                                    disabled={isProcessing}
                                                    onClick={() => handleResolveConflicts(pr)} 
                                                    className="flex-1 bg-amber-600/20 text-amber-700 dark:text-amber-500 border border-amber-600/30 font-semibold text-xs py-2.5 rounded-lg flex items-center justify-center gap-1.5 active:scale-[0.98] disabled:opacity-50"
                                                >
                                                    {isProcessing ? 'Resolving...' : (
                                                        <>
                                                            <span className="material-symbols-outlined text-[16px]">build</span>
                                                            Resolve Conflicts
                                                        </>
                                                    )}
                                                </button>
                                            )
                                        ) : (
                                            <button 
                                                disabled={isProcessing}
                                                onClick={() => handleMergeSequence(pr)} 
                                                className={`flex-1 font-bold text-xs py-2.5 rounded-lg flex items-center justify-center gap-1.5 active:scale-[0.98] disabled:opacity-50 transition-colors ${
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
            </main>
            <Link to={`/quick-issue?repo=${repo?.id}`} aria-label="Create new issue" className="fixed bottom-24 right-4 z-30 flex h-14 w-14 items-center justify-center rounded-full bg-primary text-black shadow-[0_4px_16px_rgba(19,236,37,0.4)] active:scale-90 transition-transform hover:scale-105"><span className="material-symbols-outlined text-3xl">add</span></Link>
            <BottomNav />
        </div>
    );
};

// 4. Quick Issue Overlay (Unchanged from previous logic, just receiving props)
const QuickIssuePage = ({ repos }: { repos: Repository[] }) => {
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();
    const repoId = searchParams.get('repo');
    const repo = repos.find(r => r.id === repoId) || repos[0];
    const [title, setTitle] = useState('');
    const [desc, setDesc] = useState('');
    const [isSubmitting, setIsSubmitting] = useState(false);

    const handleSubmit = async () => {
        if (!repo?.githubToken) {
            alert("No GitHub token configured for this repository.");
            return;
        }
        setIsSubmitting(true);
        try {
            githubService.initialize(repo.githubToken);
            await githubService.createIssue(repo.id, { title, description: desc, labels: [] }, repo.owner, repo.name);
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
                 <div className="w-full flex justify-center pt-4 pb-2"><div className="w-14 h-1.5 bg-gray-600/40 rounded-full"></div></div>
                 <div className="px-6 py-2 flex justify-between"><h2 className="text-white text-2xl font-bold">New Issue</h2><button onClick={() => navigate(-1)} aria-label="Close" className="size-10 rounded-full bg-white/5 flex items-center justify-center text-white"><span className="material-symbols-outlined">close</span></button></div>
                 <div className="p-6 space-y-4">
                      <input value={title} onChange={e=>setTitle(e.target.value)} className="w-full bg-input-dark rounded-xl px-4 py-4 text-white text-lg" placeholder="Title" />
                      <textarea value={desc} onChange={e=>setDesc(e.target.value)} className="w-full bg-input-dark rounded-xl p-4 text-white min-h-[200px]" placeholder="Description"></textarea>
                      <button onClick={handleSubmit} disabled={!title || isSubmitting} className="w-full bg-primary h-14 rounded-xl font-bold text-black">{isSubmitting ? 'Submitting...' : 'Submit'}</button>
                 </div>
            </div>
        </div>
    );
};

// ... BlockPromptPage and ConflictPage remain as UI stubs ...
const BlockPromptPage = () => { const navigate = useNavigate(); return <div onClick={()=>navigate(-1)} className="fixed inset-0 bg-black/80 z-50 flex items-center justify-center"><div className="bg-surface-dark p-6 rounded-xl border border-white/10 text-white">Issue Blocked. <br/><span className="text-sm text-gray-400">(Tap to dismiss)</span></div></div> };
const ConflictPage = () => { const navigate = useNavigate(); return <div className="h-screen bg-background-dark text-white p-6"><button onClick={()=>navigate(-1)} className="mb-4 text-primary">Back</button><h1>Conflicts</h1><p>Manual resolution required.</p></div> };

const App = () => {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);
  const [repos, setRepos] = useState<Repository[]>([]);

  useEffect(() => {
    const unsubscribe = onAuthStateChanged(auth, (currentUser) => {
        setUser(currentUser);
        setLoading(false);
    });
    return () => unsubscribe();
  }, []);

  useEffect(() => {
    if (user) {
        const unsubscribe = firebaseService.subscribeToRepos(user.uid, (data) => {
            setRepos(data);
        });
        return () => unsubscribe();
    }
  }, [user]);

  if (loading) return <div className="min-h-screen bg-background-dark flex items-center justify-center"><div className="size-10 border-4 border-primary border-t-transparent rounded-full animate-spin"></div></div>;

  if (!user) {
      return <LoginPage />;
  }

  return (
    <HashRouter>
      <Routes>
        <Route path="/" element={<HomePage repos={repos} user={user} />} />
        <Route path="/dashboard" element={<Dashboard repos={repos} />} />
        <Route path="/config" element={<ConfigurationPage repos={repos} setRepos={setRepos} user={user} />} />
        <Route path="/quick-issue" element={<QuickIssuePage repos={repos} />} />
        <Route path="/block/:id" element={<BlockPromptPage />} />
        <Route path="/conflicts" element={<ConflictPage />} />
      </Routes>
    </HashRouter>
  );
};

export default App;
