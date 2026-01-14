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
          maxLength={MAX_NOTE_LENGTH}
          className="w-full h-64 bg-input-dark p-4 text-white resize-none"
          placeholder="Jot down your notes here..."
        ></textarea>
        <div className="px-4 pb-4 text-right text-xs text-gray-500">
          {notes.length}/{MAX_NOTE_LENGTH}
        </div>
      </div>
    </div>
  );
};

export default Notes;
