import React, { useState, useEffect } from 'react';

const Notes = ({ isOpen, onClose, userId }: { isOpen: boolean; onClose: () => void; userId: string }) => {
  const [notes, setNotes] = useState('');
  const storageKey = `notes_${userId}`;

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

  const handleNoteChange = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
    const newValue = e.target.value;
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
    <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center">
      <div className="bg-surface-dark rounded-lg shadow-2xl w-full max-w-md mx-4">
        <div className="p-4 flex justify-between items-center border-b border-white/10">
          <h2 className="text-lg font-bold">Notes</h2>
          <button onClick={onClose} aria-label="Close Notes">
            <span className="material-symbols-outlined">close</span>
          </button>
        </div>
        <textarea
          value={notes}
          onChange={handleNoteChange}
          className="w-full h-64 bg-input-dark p-4 text-white resize-none"
          placeholder="Jot down your notes here..."
        ></textarea>
      </div>
    </div>
  );
};

export default Notes;
