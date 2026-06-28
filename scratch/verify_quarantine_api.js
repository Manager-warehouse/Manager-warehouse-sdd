const BASE_URL = 'http://127.0.0.1:8081/api/v1';

async function login(email, password) {
  const res = await fetch(`${BASE_URL}/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password })
  });
  if (!res.ok) {
    const text = await res.text();
    throw new Error(`Login failed: ${res.status} ${text}`);
  }
  const data = await res.json();
  return data.accessToken;
}

async function verify() {
  console.log('=== VERIFYING SUPABASE QUARANTINE RECORDS ===');
  try {
    const token = await login('storekeeperHN@gmail.com', 'Password@123');
    console.log('-> Login successful!');

    // Fetch quarantine items for Warehouse 2 (Hà Nội)
    const res = await fetch(`${BASE_URL}/quarantine/items?warehouseId=2`, {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    });

    if (!res.ok) {
      const text = await res.text();
      throw new Error(`Failed to fetch quarantine items: ${res.status} ${text}`);
    }

    const items = await res.json();
    console.log(JSON.stringify(items, null, 2));

    console.log('\n=============================================');
  } catch (error) {
    console.error('Verification failed:', error.message);
  }
}

verify();
