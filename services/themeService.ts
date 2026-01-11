export interface Theme {
    id: string;
    name: string;
    colors: {
        primary: string;
        primaryHover: string;
        backgroundLight: string;
        backgroundDark: string;
        surfaceDark: string;
        surfaceDarkLighter: string;
        inputDark: string;
    }
}

export const themes: Theme[] = [
    {
        id: 'pulse-green',
        name: 'Pulse Green',
        colors: {
            primary: '#12d622',
            primaryHover: '#0fa61c',
            backgroundLight: '#e8efe8',
            backgroundDark: '#0b1b0f',
            surfaceDark: '#0f1f12',
            surfaceDarkLighter: '#132618',
            inputDark: '#152b1a',
        }
    },
    {
        id: 'cyber-blue',
        name: 'Cyber Blue',
        colors: {
            primary: '#00e5ff',
            primaryHover: '#00b8d4',
            backgroundLight: '#e0f7fa',
            backgroundDark: '#001a1f',
            surfaceDark: '#00252b',
            surfaceDarkLighter: '#00333b',
            inputDark: '#00363d',
        }
    },
    {
        id: 'crimson-red',
        name: 'Crimson Red',
        colors: {
            primary: '#ff1744',
            primaryHover: '#d50000',
            backgroundLight: '#ffebee',
            backgroundDark: '#1a0505',
            surfaceDark: '#2b0b0b',
            surfaceDarkLighter: '#3b1212',
            inputDark: '#4a1515',
        }
    },
    {
        id: 'royal-purple',
        name: 'Royal Purple',
        colors: {
            primary: '#d500f9',
            primaryHover: '#aa00ff',
            backgroundLight: '#f3e5f5',
            backgroundDark: '#1a051a',
            surfaceDark: '#2b0b2b',
            surfaceDarkLighter: '#3b123b',
            inputDark: '#4a154a',
        }
    },
    {
        id: 'sunset-orange',
        name: 'Sunset Orange',
        colors: {
            primary: '#ff9100',
            primaryHover: '#ff6d00',
            backgroundLight: '#fff3e0',
            backgroundDark: '#1a1005',
            surfaceDark: '#2b1a0b',
            surfaceDarkLighter: '#3b2512',
            inputDark: '#4a3015',
        }
    },
    {
        id: 'teal-ocean',
        name: 'Teal Ocean',
        colors: {
            primary: '#1de9b6',
            primaryHover: '#00bfa5',
            backgroundLight: '#e0f2f1',
            backgroundDark: '#001a18',
            surfaceDark: '#002b26',
            surfaceDarkLighter: '#003b34',
            inputDark: '#004a42',
        }
    },
    {
        id: 'golden-hour',
        name: 'Golden Hour',
        colors: {
            primary: '#ffc400',
            primaryHover: '#ffab00',
            backgroundLight: '#fff8e1',
            backgroundDark: '#1a1605',
            surfaceDark: '#2b240b',
            surfaceDarkLighter: '#3b3112',
            inputDark: '#4a3e15',
        }
    },
    {
        id: 'pink-neon',
        name: 'Pink Neon',
        colors: {
            primary: '#ff4081',
            primaryHover: '#f50057',
            backgroundLight: '#fce4ec',
            backgroundDark: '#1a050e',
            surfaceDark: '#2b0b18',
            surfaceDarkLighter: '#3b1222',
            inputDark: '#4a152a',
        }
    },
    {
        id: 'monochrome',
        name: 'Monochrome',
        colors: {
            primary: '#ffffff',
            primaryHover: '#e0e0e0',
            backgroundLight: '#f5f5f5',
            backgroundDark: '#000000',
            surfaceDark: '#121212',
            surfaceDarkLighter: '#1e1e1e',
            inputDark: '#2c2c2c',
        }
    },
    {
        id: 'nature',
        name: 'Nature',
        colors: {
            primary: '#76ff03',
            primaryHover: '#64dd17',
            backgroundLight: '#f1f8e9',
            backgroundDark: '#0f1a05',
            surfaceDark: '#182b0b',
            surfaceDarkLighter: '#223b12',
            inputDark: '#2a4a15',
        }
    },
    {
        id: 'berry',
        name: 'Berry',
        colors: {
            primary: '#f50057',
            primaryHover: '#c51162',
            backgroundLight: '#f8bbd0',
            backgroundDark: '#1a000a',
            surfaceDark: '#2b0011',
            surfaceDarkLighter: '#3b0017',
            inputDark: '#4a001e',
        }
    },
    {
        id: 'slate',
        name: 'Slate',
        colors: {
            primary: '#64b5f6',
            primaryHover: '#42a5f5',
            backgroundLight: '#e3f2fd',
            backgroundDark: '#0d1b26',
            surfaceDark: '#152b3b',
            surfaceDarkLighter: '#1e3a4f',
            inputDark: '#264a63',
        }
    },
    {
        id: 'midnight',
        name: 'Midnight',
        colors: {
            primary: '#536dfe',
            primaryHover: '#304ffe',
            backgroundLight: '#e8eaf6',
            backgroundDark: '#050a1a',
            surfaceDark: '#0b122b',
            surfaceDarkLighter: '#121b3b',
            inputDark: '#18244a',
        }
    },
    {
        id: 'coffee',
        name: 'Coffee',
        colors: {
            primary: '#d7ccc8',
            primaryHover: '#bcaaa4',
            backgroundLight: '#efebe9',
            backgroundDark: '#1a1412',
            surfaceDark: '#2b211e',
            surfaceDarkLighter: '#3b2e2a',
            inputDark: '#4a3b36',
        }
    },
    {
        id: 'lavender',
        name: 'Lavender',
        colors: {
            primary: '#ea80fc',
            primaryHover: '#e040fb',
            backgroundLight: '#f3e5f5',
            backgroundDark: '#1a0d1a',
            surfaceDark: '#2b152b',
            surfaceDarkLighter: '#3b1d3b',
            inputDark: '#4a254a',
        }
    }
];

export const themeService = {
    applyTheme: (themeId: string) => {
        const theme = themes.find(t => t.id === themeId) || themes[0];
        const root = document.documentElement;

        root.style.setProperty('--color-primary', theme.colors.primary);
        root.style.setProperty('--color-primary-hover', theme.colors.primaryHover);
        root.style.setProperty('--color-background-light', theme.colors.backgroundLight);
        root.style.setProperty('--color-background-dark', theme.colors.backgroundDark);
        root.style.setProperty('--color-surface-dark', theme.colors.surfaceDark);
        root.style.setProperty('--color-surface-dark-lighter', theme.colors.surfaceDarkLighter);
        root.style.setProperty('--color-input-dark', theme.colors.inputDark);

        localStorage.setItem('theme-preference', themeId);
    },

    getSavedThemeId: (): string => {
        return localStorage.getItem('theme-preference') || 'pulse-green';
    },

    setDarkMode: (isDark: boolean) => {
        const root = document.documentElement;
        if (isDark) {
            root.classList.add('dark');
            localStorage.setItem('theme-mode', 'dark');
        } else {
            root.classList.remove('dark');
            localStorage.setItem('theme-mode', 'light');
        }
    },

    getSavedMode: (): boolean => {
        const saved = localStorage.getItem('theme-mode');
        if (saved) return saved === 'dark';
        return true; // Default to dark
    }
};
