import { create } from 'zustand';

// Sidebar starts open on desktop but collapsed on mobile/tablet so it doesn't
// cover the screen as a drawer on first load.
const getDefaultSidebarOpen = () => window.innerWidth >= 768;

const AUTO_DISMISS_MS = 3000;
const MAX_VISIBLE_TOASTS = 4;

const createToast = (message, type) => ({
  id: Math.random().toString(36).substring(2, 9),
  message,
  type,
});

export const useUiStore = create((set) => ({
  sidebarOpen: getDefaultSidebarOpen(),
  toasts: [],
  loading: false,

  toggleSidebar: () => set((state) => ({ sidebarOpen: !state.sidebarOpen })),
  setSidebarOpen: (open) => set({ sidebarOpen: open }),

  setLoading: (loading) => set({ loading }),

  addToast: (message, type = 'success') => {
    const toast = createToast(message, type);
    set((state) => ({
      toasts: [
        ...state.toasts.filter((t) => !(t.message === message && t.type === type)),
        toast,
      ].slice(-MAX_VISIBLE_TOASTS)
    }));

    // Auto dismiss after 3 seconds
    setTimeout(() => {
      set((state) => ({
        toasts: state.toasts.filter((t) => t.id !== toast.id)
      }));
    }, AUTO_DISMISS_MS);
  },

  removeToast: (id) => set((state) => ({
    toasts: state.toasts.filter((t) => t.id !== id)
  })),

  // Alias: showToast(type, message) — wraps addToast(message, type)
  showToast: (type, message) => {
    const toast = createToast(message, type);
    set((state) => ({
      toasts: [
        ...state.toasts.filter((t) => !(t.message === message && t.type === type)),
        toast,
      ].slice(-MAX_VISIBLE_TOASTS)
    }));
    setTimeout(() => {
      set((state) => ({
        toasts: state.toasts.filter((t) => t.id !== toast.id)
      }));
    }, AUTO_DISMISS_MS);
  },
}));
