import psycopg2

try:
    conn = psycopg2.connect(
        host="aws-1-ap-southeast-1.pooler.supabase.com",
        database="postgres",
        user="postgres.jzniugklqehtghgzggiv",
        password="Warehouse12345se12",
        port="5432"
    )
    cur = conn.cursor()
    cur.execute("SELECT u.email, u.role, a.warehouse_id FROM user_warehouse_assignments a JOIN users u ON a.user_id = u.id;")
    for row in cur.fetchall():
        print(f"Email: {row[0]}, Role: {row[1]}, Assigned Warehouse ID: {row[2]}")
    cur.close()
    conn.close()
except Exception as e:
    print("Error:", e)
