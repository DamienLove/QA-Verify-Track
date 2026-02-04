import React, { useState, useEffect } from 'react';

const Notes = ({ isOpen, onClose, userId }: { isOpen: boolean; onClose: () => void; userId: string }) => {
  const [notes, setNotes] = useState('');
  const storageKey = `notes_${userId}`;
  const MAX_NOTE_LENGTH = 10000;

  useEffect(() => {
    if (isOpen) {
      try {
        const savedNotes = localStorage.getItem(storageKey);
        if (savedNotes) {
          setNotes(savedNotes);
        } else {
          setNotes('');
        }
      } catch (e) {
        console.error('Unable to read notes from local storage', e);
        setNotes('');
      }
    }
  }, [isOpen, userId, storageKey]);

  useEffect(() => {
    const handleEsc = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose();
    };
    if (isOpen) {
      window.addEventListener('keydown', handleEsc);
    }
    return () => window.removeEventListener('keydown', handleEsc);
  }, [isOpen, onClose]);

  const handleNoteChange = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
    let newValue = e.target.value;
    if (newValue.length > MAX_NOTE_LENGTH) {
      newValue = newValue.substring(0, MAX_NOTE_LENGTH);
    }
    setNotes(newValue);
    try {
      localStorage.setItem(storageKey, newValue);
    } catch (e) {
      console.error('Unable to save notes to local storage', e);
    }
  };

  if (!isOpen) {
    return null;
  }

  return (
    <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center" onClick={onClose}>
      <div
        className="bg-surface-dark rounded-lg shadow-2xl w-full max-w-md mx-4 animate-fade-in"
        onClick={(e) => e.stopPropagation()}
        role="dialog"
        aria-modal="true"
        aria-labelledby="notes-modal-title"
      >
        <div className="p-4 flex justify-between items-center border-b border-white/10">
          <h2 id="notes-modal-title" className="text-lg font-bold">Notes</h2>
          <button onClick={onClose} aria-label="Close Notes">
            <span className="material-symbols-outlined">close</span>
          </button>
        </div>
        <textarea
          value={notes}
          onChange={handleNoteChange}
          maxLength={MAX_NOTE_LENGTH}
          className="w-full h-64 bg-input-dark p-4 text-white resize-none focus:outline-none focus:ring-2 focus:ring-primary/50"
          placeholder="Jot down your notes here..."
          autoFocus
          aria-labelledby="notes-modal-title"
          aria-describedby="notes-char-count"
        ></textarea>
        <div id="notes-char-count" className="px-4 pb-4 text-right text-xs text-gray-500">
          {notes.length}/{MAX_NOTE_LENGTH}
        </div>
      </div>
    </div>
  );
};

export default Notes;
