import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { Repository } from '../types';
import { githubService } from '../services/githubService';

interface Todo {
  repo: Repository;
  issueCount: number;
}

const Todos = ({ repos }: { repos: Repository[] }) => {
  const [todos, setTodos] = useState<Todo[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchIssueCounts = async () => {
      setLoading(true);
      const allTodos: Todo[] = [];
      for (const repo of repos) {
        if (repo.githubToken) {
          try {
            githubService.initialize(repo.githubToken);
            const issueCount = await githubService.getOpenIssueCount(repo.owner, repo.name);
            allTodos.push({ repo, issueCount });
          } catch (error) {
            console.error(`Failed to fetch issue count for ${repo.owner}/${repo.name}`, error);
          }
        }
      }
      allTodos.sort((a, b) => b.issueCount - a.issueCount);
      setTodos(allTodos);
      setLoading(false);
    };

    if (repos.length > 0) {
      fetchIssueCounts();
    } else {
      setLoading(false);
    }
  }, [repos]);

  if (loading) {
    return (
      <div className="p-4">
        <h2 className="text-lg font-bold mb-2">Todos</h2>
        <div className="text-center py-4">
          <div className="inline-block size-6 border-2 border-primary border-t-transparent rounded-full animate-spin"></div>
        </div>
      </div>
    );
  }

  if (todos.length === 0) {
    return null;
  }

  return (
    <div className="p-4">
      <h2 className="text-lg font-bold mb-2">Todos</h2>
      <div className="space-y-2">
        {todos.map(({ repo, issueCount }) => (
          <div key={repo.id} className="p-3 bg-surface-dark rounded-lg flex justify-between items-center">
            <div>
              <h3 className="font-bold">{repo.displayName || repo.name}</h3>
              <p className="text-xs text-gray-400">
                {issueCount} open issues
              </p>
            </div>
            <Link
              to={`/dashboard?repo=${repo.id}`}
              className="text-primary hover:text-primary-hover p-2 rounded-full hover:bg-white/5 transition-colors"
              aria-label={`View dashboard for ${repo.displayName || repo.name}`}
            >
              <span className="material-symbols-outlined">arrow_forward</span>
            </Link>
          </div>
        ))}
      </div>
    </div>
  );
};

export default Todos;
