import { create } from 'zustand';

// Sidebar starts open on desktop but collapsed on mobile/tablet so it doesn't
// cover the screen as a drawer on first load.
const getDefaultSidebarOpen = () => window.innerWidth >= 768;

export const useUiStore = create((set) => ({
  sidebarOpen: getDefaultSidebarOpen(),
  toasts: [],
  loading: false,

  toggleSidebar: () => set((state) => ({ sidebarOpen: !state.sidebarOpen })),
  setSidebarOpen: (open) => set({ sidebarOpen: open }),

  setLoading: (loading) => set({ loading }),

  addToast: (message, type = 'success') => {
    const id = Math.random().toString(36).substring(2, 9);
    set((state) => ({
      toasts: [...state.toasts, { id, message, type }]
    }));

    // Auto dismiss after 3 seconds
    setTimeout(() => {
      set((state) => ({
        toasts: state.toasts.filter((t) => t.id !== id)
      }));
    }, 3000);
  },

  removeToast: (id) => set((state) => ({
    toasts: state.toasts.filter((t) => t.id !== id)
  })),

  // Alias: showToast(type, message) — wraps addToast(message, type)
  showToast: (type, message) => {
    const id = Math.random().toString(36).substring(2, 9);
    set((state) => ({
      toasts: [...state.toasts, { id, message, type }]
    }));
    setTimeout(() => {
      set((state) => ({
        toasts: state.toasts.filter((t) => t.id !== id)
      }));
    }, 3000);
  },
}));
