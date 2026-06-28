const BASE_URL = 'http://127.0.0.1:8081/api/v1';

async function login(email, password) {
  const res = await fetch(`${BASE_URL}/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password })
  });
  if (!res.ok) {
    const text = await res.text();
    throw new Error(`Login failed for ${email}: ${res.status} ${text}`);
  }
  const data = await res.json();
  return data.accessToken;
}

async function request(path, method, token, body = null) {
  const headers = {
    'Content-Type': 'application/json',
  };
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }
  const options = {
    method,
    headers,
  };
  if (body) {
    options.body = JSON.stringify(body);
  }
  const res = await fetch(`${BASE_URL}${path}`, options);
  if (!res.ok) {
    const text = await res.text();
    throw new Error(`Request ${method} ${path} failed: ${res.status} ${text}`);
  }
  if (res.status === 204) return null;
  return await res.json();
}

async function findTrfIdByRequestNumber(token, requestNumber) {
  const transfers = await request('/inter-warehouse-transfers', 'GET', token);
  const found = transfers.find(t => t.externalInstructionCode === requestNumber);
  if (!found) {
    throw new Error(`InterWarehouseTransfer with externalInstructionCode ${requestNumber} not found`);
  }
  return found.id;
}

function getFutureTimeRange(hoursAheadStart, hoursAheadEnd) {
  const start = new Date();
  // Shift trip date to 2 days in the future to completely bypass today's conflicts
  start.setDate(start.getDate() + 2);
  start.setHours(start.getHours() + hoursAheadStart);
  start.setMinutes(start.getMinutes() + Math.floor(Math.random() * 50) + 5);
  
  const end = new Date(start);
  end.setHours(end.getHours() + (hoursAheadEnd - hoursAheadStart));
  
  const pad = (num) => String(num).padStart(2, '0');
  const startStr = `${start.getFullYear()}-${pad(start.getMonth() + 1)}-${pad(start.getDate())}T${pad(start.getHours())}:${pad(start.getMinutes())}:00`;
  const endStr = `${end.getFullYear()}-${pad(end.getMonth() + 1)}-${pad(end.getDate())}T${pad(end.getHours())}:${pad(end.getMinutes())}:00`;
  
  return { startStr, endStr };
}

async function runHappyPath(tokens) {
  console.log('\n--- 1. RUNNING HAPPY PATH (Full Transfer) ---');

  // Step 1: Manager kho HP tạo draft request
  console.log('Step 1: Creating Draft Transfer Request (Hải Phòng -> Hà Nội)');
  const draft = await request('/transfer-requests', 'POST', tokens.manager, {
    sourceWarehouseId: 1,
    destinationWarehouseId: 2,
    notes: "E2E Happy Path Supabase Test",
    items: [{ productId: 10, requestedQty: 2 }]
  });
  const reqId = draft.id;
  const requestNumber = draft.requestNumber;
  console.log(`-> Created Transfer Request ID: ${reqId}, Number: ${requestNumber}, Status: ${draft.status}`);

  // Step 2: Manager submit request
  console.log('Step 2: Submitting Request to CEO');
  const submitted = await request(`/transfer-requests/${reqId}/submit`, 'POST', tokens.manager);
  console.log(`-> Status: ${submitted.status}`);

  // Step 3: CEO approve request
  console.log('Step 3: CEO Approving Request');
  const approved = await request(`/transfer-requests/${reqId}/approve`, 'POST', tokens.ceo);
  console.log(`-> Status: ${approved.status}`);

  // Step 4: Planner convert request thành phiếu điều chuyển TRF
  console.log('Step 4: Planner converting Request to TRF Order');
  await request(`/transfer-requests/${reqId}/convert`, 'POST', tokens.planner);
  
  // Tìm trfId dựa trên requestNumber
  const trfId = await findTrfIdByRequestNumber(tokens.manager, requestNumber);
  console.log(`-> Converted Transfer ID: ${trfId}`);

  // Lấy chi tiết TRF để có transfer_item_id
  const trfDetail = await request(`/inter-warehouse-transfers/${trfId}`, 'GET', tokens.manager);
  const itemId = trfDetail.items[0].id;
  console.log(`-> Transfer Item ID: ${itemId}`);

  // Step 5: Manager nguồn HP approve phiếu TRF
  console.log('Step 5: Manager approving TRF Order (Reserving stock)');
  await request(`/inter-warehouse-transfers/${trfId}/approve`, 'POST', tokens.manager);

  // Step 6: Dispatcher assign Trip (vehicle=2, driver=1)
  console.log('Step 6: Dispatcher assigning vehicle 2 & driver 1 to Trip');
  const times = getFutureTimeRange(1, 2);
  await request(`/inter-warehouse-transfers/${trfId}/trip`, 'POST', tokens.dispatcher, {
    vehicleId: 2,
    driverId: 1,
    plannedStartAt: times.startStr,
    plannedEndAt: times.endStr
  });

  // Step 7: Storekeeper HP ship hàng (Loading)
  console.log('Step 7: Storekeeper shipping TRF');
  await request(`/inter-warehouse-transfers/${trfId}/ship`, 'POST', tokens.storekeeper);

  // Step 8: Driver depart xe (In-transit)
  console.log('Step 8: Driver confirming departure');
  await request(`/inter-warehouse-transfers/${trfId}/depart`, 'POST', tokens.driver);

  // Step 9: Nhân viên nhận HN ghi nhận receiveCount = 2
  console.log('Step 9: Destination staff counting received goods (Count: 2)');
  await request(`/inter-warehouse-transfers/${trfId}/receive-count`, 'PUT', tokens.staffHN, {
    items: [{ transferItemId: itemId, receivedQty: 2, issueReason: "" }]
  });

  // Step 10: Storekeeper HN check QC (qcPassed = 2, qcFailed = 0)
  console.log('Step 10: Storekeeper checking QC (Pass: 2, Fail: 0)');
  await request(`/inter-warehouse-transfers/${trfId}/receive-check`, 'PUT', tokens.storekeeperHN, {
    items: [{
      transferItemId: itemId,
      confirmedQty: 2,
      qcPassedQty: 2,
      qcFailedQty: 0,
      destinationLocationId: 18, // HN bin location
      checkerNote: "Happy path OK",
      qcFailureReason: ""
    }]
  });

  // Step 11: Manager HN finalReceive
  console.log('Step 11: Manager finalizing receiving process');
  await request(`/inter-warehouse-transfers/${trfId}/final-receive`, 'POST', tokens.manager, {
    discrepancyReason: ""
  });
  console.log('=> HAPPY PATH COMPLETED SUCCESSFULLY!');
  return trfId;
}

async function runShortagePath(tokens) {
  console.log('\n--- 2. RUNNING EXCEPTION PATH: SHORTAGE ---');

  // Step 1: Manager HP tạo draft
  console.log('Step 1: Creating Draft Request (Qty: 5)');
  const draft = await request('/transfer-requests', 'POST', tokens.manager, {
    sourceWarehouseId: 1,
    destinationWarehouseId: 2,
    notes: "E2E Shortage Supabase Test",
    items: [{ productId: 10, requestedQty: 5 }]
  });
  const reqId = draft.id;
  const requestNumber = draft.requestNumber;

  // Submit -> Approve -> Convert
  await request(`/transfer-requests/${reqId}/submit`, 'POST', tokens.manager);
  await request(`/transfer-requests/${reqId}/approve`, 'POST', tokens.ceo);
  await request(`/transfer-requests/${reqId}/convert`, 'POST', tokens.planner);
  
  const trfId = await findTrfIdByRequestNumber(tokens.manager, requestNumber);
  const trfDetail = await request(`/inter-warehouse-transfers/${trfId}`, 'GET', tokens.manager);
  const itemId = trfDetail.items[0].id;

  // Approve -> Assign Trip -> Ship -> Depart
  await request(`/inter-warehouse-transfers/${trfId}/approve`, 'POST', tokens.manager);
  const times = getFutureTimeRange(3, 4);
  await request(`/inter-warehouse-transfers/${trfId}/trip`, 'POST', tokens.dispatcher, {
    vehicleId: 2,
    driverId: 1,
    plannedStartAt: times.startStr,
    plannedEndAt: times.endStr
  });
  await request(`/inter-warehouse-transfers/${trfId}/ship`, 'POST', tokens.storekeeper);
  await request(`/inter-warehouse-transfers/${trfId}/depart`, 'POST', tokens.driver);

  // Step 9: Thực nhận 3 (thiếu 2)
  console.log('Step 9: Destination staff counting received goods (Count: 3, Shortage: 2)');
  await request(`/inter-warehouse-transfers/${trfId}/receive-count`, 'PUT', tokens.staffHN, {
    items: [{ transferItemId: itemId, receivedQty: 3, issueReason: "Shortage" }]
  });

  // Step 10: Storekeeper check QC (confirmed = 3, pass = 3, fail = 0)
  console.log('Step 10: Storekeeper checking QC (Pass: 3, Fail: 0)');
  await request(`/inter-warehouse-transfers/${trfId}/receive-check`, 'PUT', tokens.storekeeperHN, {
    items: [{
      transferItemId: itemId,
      confirmedQty: 3,
      qcPassedQty: 3,
      qcFailedQty: 0,
      destinationLocationId: 18,
      checkerNote: "Lost 2 items",
      qcFailureReason: ""
    }]
  });

  // Step 11: Final receive with discrepancy reason
  console.log('Step 11: Manager finalizing receiving with discrepancy');
  await request(`/inter-warehouse-transfers/${trfId}/final-receive`, 'POST', tokens.manager, {
    discrepancyReason: "Shortage variance: lost 2 units"
  });
  console.log('=> SHORTAGE PATH COMPLETED SUCCESSFULLY!');
  return trfId;
}

async function runWrongSkuPath(tokens) {
  console.log('\n--- 3. RUNNING EXCEPTION PATH: WRONG SKU (Return) ---');

  // Step 1: Manager HP tạo draft
  console.log('Step 1: Creating Draft Request (Qty: 3)');
  const draft = await request('/transfer-requests', 'POST', tokens.manager, {
    sourceWarehouseId: 1,
    destinationWarehouseId: 2,
    notes: "E2E Wrong SKU Supabase Test",
    items: [{ productId: 10, requestedQty: 3 }]
  });
  const reqId = draft.id;
  const requestNumber = draft.requestNumber;

  // Submit -> Approve -> Convert
  await request(`/transfer-requests/${reqId}/submit`, 'POST', tokens.manager);
  await request(`/transfer-requests/${reqId}/approve`, 'POST', tokens.ceo);
  await request(`/transfer-requests/${reqId}/convert`, 'POST', tokens.planner);
  
  const trfId = await findTrfIdByRequestNumber(tokens.manager, requestNumber);

  // Approve -> Assign Trip -> Ship -> Depart
  await request(`/inter-warehouse-transfers/${trfId}/approve`, 'POST', tokens.manager);
  const times = getFutureTimeRange(5, 6);
  await request(`/inter-warehouse-transfers/${trfId}/trip`, 'POST', tokens.dispatcher, {
    vehicleId: 2,
    driverId: 1,
    plannedStartAt: times.startStr,
    plannedEndAt: times.endStr
  });
  await request(`/inter-warehouse-transfers/${trfId}/ship`, 'POST', tokens.storekeeper);
  await request(`/inter-warehouse-transfers/${trfId}/depart`, 'POST', tokens.driver);

  // Step 9: Storekeeper HN phát hiện sai SKU, yêu cầu trả hàng
  console.log('Step 9: Storekeeper requesting return due to wrong SKU');
  await request(`/inter-warehouse-transfers/${trfId}/request-return`, 'POST', tokens.storekeeperHN, {
    reason: "Wrong SKU delivered: expected SKU-TRF-001, got wrong items"
  });

  // Step 10: Manager HP duyệt return
  console.log('Step 10: Source manager approving return');
  await request(`/inter-warehouse-transfers/${trfId}/approve-return`, 'POST', tokens.manager);

  // Step 11: Manager HP xác nhận hàng về kho nguồn Hải Phòng
  console.log('Step 11: Source manager confirming arrival back at source');
  await request(`/inter-warehouse-transfers/${trfId}/return-to-source`, 'POST', tokens.manager);
  console.log('=> WRONG SKU RETURN PATH COMPLETED SUCCESSFULLY!');
  return trfId;
}

async function runQuarantineAndDisposalPath(tokens) {
  console.log('\n--- 4. RUNNING EXCEPTION PATH: PHYSICAL DAMAGE & QUARANTINE ---');

  // Step 1: Manager HP tạo draft (Qty: 4)
  console.log('Step 1: Creating Draft Request (Qty: 4)');
  const draft = await request('/transfer-requests', 'POST', tokens.manager, {
    sourceWarehouseId: 1,
    destinationWarehouseId: 2,
    notes: "E2E Quarantine Supabase Test",
    items: [{ productId: 10, requestedQty: 4 }]
  });
  const reqId = draft.id;
  const requestNumber = draft.requestNumber;

  // Submit -> Approve -> Convert
  await request(`/transfer-requests/${reqId}/submit`, 'POST', tokens.manager);
  await request(`/transfer-requests/${reqId}/approve`, 'POST', tokens.ceo);
  await request(`/transfer-requests/${reqId}/convert`, 'POST', tokens.planner);
  
  const trfId = await findTrfIdByRequestNumber(tokens.manager, requestNumber);
  const trfDetail = await request(`/inter-warehouse-transfers/${trfId}`, 'GET', tokens.manager);
  const itemId = trfDetail.items[0].id;

  // Approve -> Assign Trip -> Ship -> Depart
  await request(`/inter-warehouse-transfers/${trfId}/approve`, 'POST', tokens.manager);
  const times = getFutureTimeRange(7, 8);
  await request(`/inter-warehouse-transfers/${trfId}/trip`, 'POST', tokens.dispatcher, {
    vehicleId: 2,
    driverId: 1,
    plannedStartAt: times.startStr,
    plannedEndAt: times.endStr
  });
  await request(`/inter-warehouse-transfers/${trfId}/ship`, 'POST', tokens.storekeeper);
  await request(`/inter-warehouse-transfers/${trfId}/depart`, 'POST', tokens.driver);

  // Step 9: Đếm 4
  console.log('Step 9: Destination staff counting received goods (Count: 4)');
  await request(`/inter-warehouse-transfers/${trfId}/receive-count`, 'PUT', tokens.staffHN, {
    items: [{ transferItemId: itemId, receivedQty: 4, issueReason: "" }]
  });

  // Step 10: Check QC, phát hiện hỏng 1, đạt 3
  console.log('Step 10: Storekeeper checking QC (Pass: 3, Fail: 1, locationId=18)');
  await request(`/inter-warehouse-transfers/${trfId}/receive-check`, 'PUT', tokens.storekeeperHN, {
    items: [{
      transferItemId: itemId,
      confirmedQty: 4,
      qcPassedQty: 3,
      qcFailedQty: 1,
      destinationLocationId: 18,
      checkerNote: "1 item broken",
      qcFailureReason: "Physical damage during transport"
    }]
  });

  // Step 11: Final receive
  console.log('Step 11: Manager finalizing receiving');
  await request(`/inter-warehouse-transfers/${trfId}/final-receive`, 'POST', tokens.manager, {
    discrepancyReason: "1 item failed QC"
  });

  console.log('=> QUARANTINE PATH COMPLETED SUCCESSFULLY!');
  return trfId;
}

async function main() {
  console.log('=== STARTING WMS E2E SUPABASE TESTING ===');
  
  try {
    console.log('Authenticating test users...');
    const tokens = {
      manager: await login('whHP-HN@gmail.com', 'Password@123'),
      ceo: await login('ceo@phucanh.vn', 'Password@123'),
      planner: await login('planer@gmail.com', 'Password@123'),
      storekeeper: await login('storekeeperHP@gmail.com', 'Password@123'),
      storekeeperHN: await login('storekeeperHN@gmail.com', 'Password@123'),
      staffHN: await login('staff@gmail.com', 'Password@123'),
      dispatcher: await login('dispatcher-HP@gmail.com', 'Password@123'),
      driver: await login('driver_test@wms.com', 'Password@123')
    };
    console.log('-> Authentication successful for all users!');

    const happyTrfId = await runHappyPath(tokens);
    const shortageTrfId = await runShortagePath(tokens);
    const wrongSkuTrfId = await runWrongSkuPath(tokens);
    const quarantineTrfId = await runQuarantineAndDisposalPath(tokens);

    console.log('\n=============================================');
    console.log('=== ALL E2E FLOWS RUN COMPLETED ON API ===');
    console.log(`Happy Path TRF ID: ${happyTrfId}`);
    console.log(`Shortage TRF ID: ${shortageTrfId}`);
    console.log(`Wrong SKU TRF ID: ${wrongSkuTrfId}`);
    console.log(`Quarantine TRF ID: ${quarantineTrfId}`);
    console.log('=============================================');

  } catch (error) {
    console.error('ERROR DURING E2E RUN:', error);
  }
}

main();
