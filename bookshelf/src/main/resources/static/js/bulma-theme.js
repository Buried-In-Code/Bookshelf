const STORAGE_KEY = 'bookshelf-theme';
const colourScheme = window.matchMedia('(prefers-color-scheme: dark)');

function getSystemTheme() {
  return colourScheme.matches ? 'dark' : 'light';
}

function applyTheme(theme) {
  document.documentElement.setAttribute('data-theme', theme);
}

function getSavedTheme() {
  const savedTheme = localStorage.getItem(STORAGE_KEY);
  if (savedTheme === 'dark' || savedTheme === 'light' || savedTheme == 'system') {
    return savedTheme;
  }
  return 'system';
}

function resolveTheme(theme) {
  return theme === 'system' ? getSystemTheme() : theme;
}

function setSavedTheme(theme) {
  localStorage.setItem(STORAGE_KEY, theme);
  applyTheme(resolveTheme(theme));
}

function syncThemeSelector(theme) {
  const themeSelector = document.getElementById('theme-selector');
  if (themeSelector) {
    themeSelector.value = theme;
  }
}

colourScheme.addEventListener('change', () => {
  const current = getSavedTheme();
  if (current === 'system') {
    applyTheme(getSystemTheme());
  }
});

document.addEventListener('DOMContentLoaded', () => {
  const themeSelector = document.getElementById('theme-selector');
  const initialTheme = getSavedTheme();
  applyTheme(resolveTheme(initialTheme));
  syncThemeSelector(initialTheme);
  if (themeSelector) {
    themeSelector.addEventListener('change', e => {
      setSavedTheme(e.target.value);
    });
  }
});
