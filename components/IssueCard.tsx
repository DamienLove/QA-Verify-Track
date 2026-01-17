import React, { memo } from 'react';
import { Link } from 'react-router-dom';
import { Issue } from '../types';

interface IssueCardProps {
    issue: Issue;
    repoId: string | number;
    isBusy: boolean;
    isAnalyzing: boolean;
    analysisResult?: string;
    onFixed: (id: number, number: number) => void;
    onOpen: (id: number, number: number) => void;
    onBlock: (issue: Issue) => void;
    onAnalyze: (issue: Issue) => void;
}

export const IssueCard = memo(({
    issue,
    repoId,
    isBusy,
    isAnalyzing,
    analysisResult,
    onFixed,
    onOpen,
    onBlock,
    onAnalyze
}: IssueCardProps) => {
    return (
        <article className="relative flex flex-col gap-1.5 rounded-lg bg-white dark:bg-surface-dark-lighter/80 p-2 shadow-sm border border-gray-100 dark:border-white/10 animate-fade-in">
            <div className="flex items-start justify-between gap-2.5">
                <div className="flex flex-col gap-1">
                    <div className="flex items-center gap-2">
                        <span className={`font-bold text-xs tracking-tight flex items-center gap-1 uppercase ${issue.priority === 'critical' ? 'text-red-500' : issue.priority === 'high' ? 'text-orange-500' : 'text-blue-400'}`}>
                            <span className="material-symbols-outlined text-[14px] fill-1">{issue.priority === 'critical' ? 'error' : 'flag'}</span>{issue.priority}
                        </span>
                        <span className="text-gray-500 dark:text-gray-500 text-[11px] font-medium">#{issue.number}</span>
                    </div>
                    <Link to={`/issue/${repoId}/${issue.number}`} className="text-base font-semibold text-slate-900 dark:text-white leading-snug hover:text-primary">
                        {issue.title}
                    </Link>
                </div>
            </div>
            <div className="bg-gray-50 dark:bg-black/30 rounded-lg p-1.5">
                <p className="text-xs text-gray-600 dark:text-gray-200 line-clamp-3 font-mono leading-snug">{issue.description}</p>
            </div>

            {/* AI Analysis Result Display */}
            {analysisResult && (
                <div className="bg-primary/10 border border-primary/25 rounded-lg p-2 animate-fade-in">
                    <div className="flex items-center gap-1 mb-1 text-primary">
                        <span className="material-symbols-outlined text-[15px]">smart_toy</span>
                        <span className="text-[11px] font-bold uppercase">Gemini Analysis</span>
                    </div>
                    <p className="text-[11px] text-slate-800 dark:text-gray-100 whitespace-pre-wrap leading-relaxed">{analysisResult}</p>
                </div>
            )}

            <div className="grid grid-cols-4 gap-1.5 mt-0.5">
                <button disabled={isBusy} onClick={() => onFixed(issue.id, issue.number)} className="col-span-1 flex flex-col items-center justify-center gap-1 h-9 rounded-lg bg-primary text-black font-semibold text-[11px] shadow-[0_2px_8px_rgba(19,236,37,0.25)] active:scale-95 transition-transform disabled:opacity-50"><span className="material-symbols-outlined text-[18px]">check_circle</span>Fixed</button>
                <button disabled={isBusy} onClick={() => onOpen(issue.id, issue.number)} className="col-span-1 flex flex-col items-center justify-center gap-1 h-9 rounded-lg bg-orange-500 text-white font-semibold text-[11px] shadow-[0_2px_8px_rgba(249,115,22,0.25)] active:scale-95 transition-transform disabled:opacity-50"><span className="material-symbols-outlined text-[18px]">warning</span>Open</button>
                <button disabled={isBusy} onClick={() => onBlock(issue)} className="col-span-1 flex flex-col items-center justify-center gap-1 h-9 rounded-lg bg-red-600 text-white font-semibold text-[11px] shadow-[0_2px_8px_rgba(220,38,38,0.25)] active:scale-95 transition-transform disabled:opacity-50"><span className="material-symbols-outlined text-[18px]">block</span>Blocked</button>
                <button
                    onClick={() => onAnalyze(issue)}
                    disabled={isAnalyzing}
                    className="col-span-1 flex flex-col items-center justify-center gap-1 h-9 rounded-lg bg-blue-600/10 dark:bg-blue-500/10 text-blue-600 dark:text-blue-300 border border-blue-600/20 dark:border-blue-500/25 font-semibold text-[11px] active:scale-95 transition-transform disabled:opacity-50"
                >
                    {isAnalyzing ? (
                        <span className="size-4 border-2 border-current border-t-transparent rounded-full animate-spin"></span>
                    ) : (
                        <span className="material-symbols-outlined text-[18px]">analytics</span>
                    )}
                    Analyze
                </button>
            </div>
        </article>
    );
});
