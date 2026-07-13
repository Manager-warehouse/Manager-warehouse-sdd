import { useUiStore } from '../../src/stores/ui.store';

describe('useUiStore', () => {
  beforeEach(() => {
    // Reset state before each test
    useUiStore.setState({
      sidebarOpen: true,
      toasts: [],
      loading: false
    });
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  test('toggleSidebar toggles the sidebarOpen state', () => {
    expect(useUiStore.getState().sidebarOpen).toBe(true);
    useUiStore.getState().toggleSidebar();
    expect(useUiStore.getState().sidebarOpen).toBe(false);
    useUiStore.getState().toggleSidebar();
    expect(useUiStore.getState().sidebarOpen).toBe(true);
  });

  test('setSidebarOpen updates sidebarOpen state', () => {
    useUiStore.getState().setSidebarOpen(false);
    expect(useUiStore.getState().sidebarOpen).toBe(false);
    useUiStore.getState().setSidebarOpen(true);
    expect(useUiStore.getState().sidebarOpen).toBe(true);
  });

  test('setLoading updates loading state', () => {
    expect(useUiStore.getState().loading).toBe(false);
    useUiStore.getState().setLoading(true);
    expect(useUiStore.getState().loading).toBe(true);
  });

  test('addToast adds a toast and auto-dismisses it after 3s', () => {
    expect(useUiStore.getState().toasts).toHaveLength(0);
    
    useUiStore.getState().addToast('Test Message', 'error');
    
    const toasts = useUiStore.getState().toasts;
    expect(toasts).toHaveLength(1);
    expect(toasts[0].message).toBe('Test Message');
    expect(toasts[0].type).toBe('error');
    expect(toasts[0].id).toBeDefined();

    // Fast-forward 3 seconds
    vi.advanceTimersByTime(3000);
    expect(useUiStore.getState().toasts).toHaveLength(0);
  });

  test('removeToast removes a specific toast by id', () => {
    useUiStore.getState().addToast('Toast 1');
    useUiStore.getState().addToast('Toast 2');
    
    const toasts = useUiStore.getState().toasts;
    expect(toasts).toHaveLength(2);
    
    const firstId = toasts[0].id;
    useUiStore.getState().removeToast(firstId);
    
    expect(useUiStore.getState().toasts).toHaveLength(1);
    expect(useUiStore.getState().toasts[0].message).toBe('Toast 2');
  });

  test('showToast adds a toast using alias parameters and auto-dismisses it', () => {
    useUiStore.getState().showToast('info', 'Info Toast');
    
    const toasts = useUiStore.getState().toasts;
    expect(toasts).toHaveLength(1);
    expect(toasts[0].message).toBe('Info Toast');
    expect(toasts[0].type).toBe('info');

    // Fast-forward 3 seconds
    vi.advanceTimersByTime(3000);
    expect(useUiStore.getState().toasts).toHaveLength(0);
  });
});
