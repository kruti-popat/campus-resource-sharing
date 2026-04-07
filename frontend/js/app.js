// API Base URL
const API_BASE_URL = 'http://localhost:8080/api';

// Current User
let currentUser = null;

// Retry helper function
async function fetchWithRetry(url, options = {}, retries = 3) {
    for (let i = 0; i < retries; i++) {
        try {
            const response = await fetch(url, {
                ...options,
                cache: 'no-cache',
                headers: {
                    'Content-Type': 'application/json',
                    ...options.headers
                }
            });
            return response;
        } catch (error) {
            if (i === retries - 1) throw error;
            // Wait before retry (100ms, 300ms, 500ms)
            await new Promise(resolve => setTimeout(resolve, 100 * (i + 1) * 2));
        }
    }
}

// Initialize app
document.addEventListener('DOMContentLoaded', function() {
    // Check if user is logged in
    const userData = localStorage.getItem('currentUser');
    if (userData) {
        currentUser = JSON.parse(userData);
        // Redirect to dashboard if on index page
        if (window.location.pathname.includes('index.html') || window.location.pathname.endsWith('/')) {
            window.location.href = 'dashboard.html';
        } else if (window.location.pathname.includes('dashboard.html')) {
            initializeDashboard();
        }
    } else {
        // Redirect to index if not logged in and on dashboard
        if (window.location.pathname.includes('dashboard.html')) {
            window.location.href = 'index.html';
        }
    }
});

// ========== AUTHENTICATION ==========

let pendingOtpEmail = null;
let otpTimerInterval = null;

function showLogin() {
    document.getElementById('loginForm').classList.add('active');
    document.getElementById('registerForm').classList.remove('active');
    document.getElementById('otpForm').classList.remove('active');
    document.querySelectorAll('.tab-btn')[0].classList.add('active');
    document.querySelectorAll('.tab-btn')[1].classList.remove('active');
    // Update heading for Login
    var title = document.getElementById('authTitle');
    var subtitle = document.getElementById('authSubtitle');
    if (title) title.textContent = 'Welcome Back';
    if (subtitle) subtitle.textContent = 'Sign in to your account or create a new one';
    clearOtpTimer();
}

function showRegister() {
    document.getElementById('registerForm').classList.add('active');
    document.getElementById('loginForm').classList.remove('active');
    document.getElementById('otpForm').classList.remove('active');
    document.querySelectorAll('.tab-btn')[1].classList.add('active');
    document.querySelectorAll('.tab-btn')[0].classList.remove('active');
    // Update heading for Register
    var title = document.getElementById('authTitle');
    var subtitle = document.getElementById('authSubtitle');
    if (title) title.textContent = 'Create Account';
    if (subtitle) subtitle.textContent = 'Join the campus community today';
    clearOtpTimer();
}

function showOtpForm(email) {
    document.getElementById('loginForm').classList.remove('active');
    document.getElementById('registerForm').classList.remove('active');
    document.getElementById('otpForm').classList.add('active');
    document.getElementById('otpEmail').textContent = email;
    document.getElementById('otpInput').value = '';
    document.getElementById('otpInput').focus();
    pendingOtpEmail = email;
    startOtpTimer();
}

async function handleLogin(event) {
    event.preventDefault();
    const username = document.getElementById('loginUsername').value;
    const password = document.getElementById('loginPassword').value;
    
    if (!username || !password) {
        showMessage('Please enter username and password', 'error');
        return;
    }
    
    // Clear any existing session data
    localStorage.clear();
    currentUser = null;
    
    try {
        const response = await fetchWithRetry(`${API_BASE_URL}/users/login`, {
            method: 'POST',
            body: JSON.stringify({ username, password })
        });
        
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        
        const data = await response.json();
        
        if (data.success) {
            currentUser = data.data;
            localStorage.setItem('currentUser', JSON.stringify(currentUser));
            showMessage('Login successful!', 'success');
            // Immediate redirect without delay
            window.location.href = 'dashboard.html';
        } else {
            showMessage(data.message || 'Login failed', 'error');
        }
    } catch (error) {
        console.error('Login error:', error);
        showMessage('Cannot connect to server. Please ensure backend is running.', 'error');
    }
}

async function handleRegister(event) {
    event.preventDefault();
    const username = document.getElementById('regUsername').value;
    const email = document.getElementById('regEmail').value;
    const password = document.getElementById('regPassword').value;
    const fullName = document.getElementById('regFullName').value;
    const phone = document.getElementById('regPhone').value;
    
    try {
        const response = await fetch(`${API_BASE_URL}/users/register`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ username, email, password, fullName, phone })
        });
        
        const data = await response.json();
        
        if (data.success) {
            showMessage('OTP sent to your email!', 'success');
            showOtpForm(email);
        } else {
            showMessage(data.message || 'Registration failed', 'error');
        }
    } catch (error) {
        showMessage('Error connecting to server', 'error');
    }
}

async function handleVerifyOtp(event) {
    event.preventDefault();
    const otp = document.getElementById('otpInput').value;
    
    if (!otp || otp.length !== 6) {
        showMessage('Please enter a valid 6-digit OTP', 'error');
        return;
    }
    
    try {
        const response = await fetch(`${API_BASE_URL}/users/verify-otp`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ email: pendingOtpEmail, otp: otp })
        });
        
        const data = await response.json();
        
        if (data.success) {
            showMessage('Email verified! Registration successful. Please login.', 'success');
            clearOtpTimer();
            pendingOtpEmail = null;
            setTimeout(() => {
                showLogin();
            }, 1500);
        } else {
            showMessage(data.message || 'OTP verification failed', 'error');
        }
    } catch (error) {
        showMessage('Error connecting to server', 'error');
    }
}

async function handleResendOtp() {
    if (!pendingOtpEmail) {
        showMessage('No pending registration. Please register again.', 'error');
        showRegister();
        return;
    }
    
    const resendBtn = document.getElementById('resendOtpBtn');
    resendBtn.disabled = true;
    resendBtn.textContent = 'Sending...';
    
    try {
        const response = await fetch(`${API_BASE_URL}/users/resend-otp`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ email: pendingOtpEmail })
        });
        
        const data = await response.json();
        
        if (data.success) {
            showMessage('OTP resent to your email!', 'success');
            startOtpTimer();
        } else {
            showMessage(data.message || 'Failed to resend OTP', 'error');
        }
    } catch (error) {
        showMessage('Error connecting to server', 'error');
    } finally {
        resendBtn.disabled = false;
        resendBtn.textContent = 'Resend OTP';
    }
}

function startOtpTimer() {
    clearOtpTimer();
    let seconds = 30;
    const timerEl = document.getElementById('otpTimer');
    const resendBtn = document.getElementById('resendOtpBtn');
    resendBtn.disabled = true;
    resendBtn.style.opacity = '0.5';
    
    timerEl.textContent = `Resend in ${seconds}s`;
    
    otpTimerInterval = setInterval(() => {
        seconds--;
        if (seconds <= 0) {
            clearOtpTimer();
            timerEl.textContent = '';
            resendBtn.disabled = false;
            resendBtn.style.opacity = '1';
        } else {
            timerEl.textContent = `Resend in ${seconds}s`;
        }
    }, 1000);
}

function clearOtpTimer() {
    if (otpTimerInterval) {
        clearInterval(otpTimerInterval);
        otpTimerInterval = null;
    }
}

function handleLogout() {
    document.getElementById('logoutConfirm').classList.add('active');
}

function closeLogoutConfirm() {
    document.getElementById('logoutConfirm').classList.remove('active');
}

function confirmLogout() {
    // Clear all localStorage data
    localStorage.clear();
    currentUser = null;
    window.location.href = 'index.html';
}

// ========== UTILITY FUNCTIONS ==========

// Helper function to generate transaction info HTML
function getTransactionInfoHTML(resource) {
    if (resource.transactionType === 'SELL') {
        return `<p class="transaction-info" style="color: #28a745; font-weight: 600;">💰 For Sale: ₹${resource.price || 'N/A'}</p>`;
    } else if (resource.transactionType === 'EXCHANGE') {
        return `<p class="transaction-info" style="color: #667eea; font-weight: 600;">🔄 For Exchange: ${resource.exchangeDescription || 'See details'}</p>`;
    } else if (resource.transactionType === 'RENT') {
        return `<p class="transaction-info" style="color: #ffc107; font-weight: 600;">📅 For Rent: ₹${resource.price || 'N/A'} / ${resource.rentalDuration || 'N/A'}</p>`;
    } else if (resource.transactionType === 'DONATE') {
        return `<p class="transaction-info" style="color: #dc3545; font-weight: 600;">🎁 Free Donation</p>`;
    }
    return '';
}

// ========== DASHBOARD ==========

function initializeDashboard() {
    if (!currentUser) {
        window.location.href = 'index.html';
        return;
    }
    
    document.getElementById('userName').textContent = currentUser.fullName || currentUser.username;
    loadResources();
    loadMyResources();
    loadMyRequests().then(function() { updateChatBadges(); });
    loadReceivedRequests().then(function() { updateChatBadges(); });
    loadUserRatingBadge();
    
    // Start periodic badge updates (every 5 seconds)
    startPeriodicBadgeUpdates();
}

async function loadUserRatingBadge() {
    try {
        const response = await fetch(`${API_BASE_URL}/users/${currentUser.id}`);
        const data = await response.json();
        
        if (data.success) {
            const user = data.data;
            updateNavbarRating(user.averageRating || 0, user.totalRatings || 0);
        }
    } catch (error) {
        console.error('Error loading user rating:', error);
    }
}

async function loadResources() {
    try {
        const response = await fetchWithRetry(`${API_BASE_URL}/resources`, {
            method: 'GET'
        });
        const data = await response.json();
        
        if (data.success) {
            displayResources(data.data);
        }
    } catch (error) {
        console.error('Error loading resources:', error);
        showMessage('Error loading resources', 'error');
    }
}

function displayResources(resources) {
    const container = document.getElementById('resourcesContainer');
    container.innerHTML = '';
    
    if (resources.length === 0) {
        container.innerHTML = '<p style="text-align: center; color: #666;">No resources available</p>';
        return;
    }
    
    resources.forEach(resource => {
        // Don't show resources owned by current user
        if (resource.owner.id === currentUser.id) {
            return;
        }
        
        const card = document.createElement('div');
        card.className = 'resource-card';
        
        let actionButton = '';
        if (resource.alreadyRequested) {
            actionButton = '<button class="btn btn-secondary" disabled>Already Requested</button>';
        } else {
            actionButton = `<button class="btn btn-primary" onclick="openRequestModal(${resource.id})">Request</button>`;
        }
        
        const transactionInfo = getTransactionInfoHTML(resource);
        const ownerRating = resource.owner.averageRating ? `⭐ ${resource.owner.averageRating.toFixed(1)}` : '';
        
        card.innerHTML = `
            <h3>${resource.title}</h3>
            <span class="category">${resource.category}</span>
            ${transactionInfo}
            <p class="condition">Condition: ${resource.conditionStatus}</p>
            <p class="description">${resource.description || 'No description'}</p>
            <p class="owner">
                Owner: <a href="#" class="owner-link" onclick="viewUserProfile(${resource.owner.id}); return false;">${resource.owner.fullName}</a>
                <span class="owner-rating">${ownerRating}</span>
            </p>
            <div class="actions">
                ${actionButton}
            </div>
        `;
        container.appendChild(card);
    });
}

async function filterResources() {
    const category = document.getElementById('categoryFilter').value;
    try {
        const url = category ? `${API_BASE_URL}/resources?category=${category}` : `${API_BASE_URL}/resources`;
        const response = await fetch(url);
        const data = await response.json();
        
        if (data.success) {
            displayResources(data.data);
        }
    } catch (error) {
        console.error('Error filtering resources:', error);
    }
}

// ========== MY RESOURCES ==========

async function loadMyResources() {
    try {
        const response = await fetchWithRetry(`${API_BASE_URL}/resources/owner/${currentUser.id}`, {
            method: 'GET'
        });
        const data = await response.json();
        
        if (data.success) {
            displayMyResources(data.data);
        }
    } catch (error) {
        console.error('Error loading my resources:', error);
    }
}

function displayMyResources(resources) {
    const container = document.getElementById('myResourcesContainer');
    container.innerHTML = '';
    
    if (resources.length === 0) {
        container.innerHTML = '<p style="text-align: center; color: #666;">You haven\'t added any resources yet</p>';
        return;
    }
    
    resources.forEach(resource => {
        const transactionInfo = getTransactionInfoHTML(resource);
        const card = document.createElement('div');
        card.className = 'resource-card';
        card.innerHTML = `
            <h3>${resource.title}</h3>
            <span class="category">${resource.category}</span>
            ${transactionInfo}
            <p class="condition">Condition: ${resource.conditionStatus}</p>
            <p class="description">${resource.description || 'No description'}</p>
            <p class="owner">Status: ${resource.isAvailable ? 'Available' : 'Not Available'}</p>
        `;
        container.appendChild(card);
    });
}

// ========== REQUESTS ==========

async function loadMyRequests() {
    if (!currentUser || !currentUser.id) {
        return;
    }
    
    try {
        const response = await fetchWithRetry(`${API_BASE_URL}/requests/requester/${currentUser.id}`, {
            method: 'GET'
        });
        
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        
        const data = await response.json();
        
        if (data.success) {
            displayMyRequests(data.data);
        } else {
            console.error('Failed to load requests:', data.message);
        }
    } catch (error) {
        console.error('Error loading my requests:', error);
    }
}

function displayMyRequests(requests) {
    const container = document.getElementById('myRequestsContainer');
    
    if (!container) {
        return;
    }
    
    container.innerHTML = '';
    
    if (!requests || !Array.isArray(requests)) {
        container.innerHTML = '<p style="text-align: center; color: #666;">Error loading requests</p>';
        return;
    }
    
    if (requests.length === 0) {
        container.innerHTML = '<p style="text-align: center; color: #666;">You haven\'t made any requests</p>';
        return;
    }
    
    requests.forEach((request) => {
        try {
            if (!request) {
                return;
            }
            
            const item = document.createElement('div');
            item.className = 'request-item';
            
            const resourceTitle = request.resource?.title || 'Unknown Resource';
            const resourceCategory = request.resource?.category || 'N/A';
            const ownerFullName = request.owner?.fullName || request.owner?.username || 'Unknown';
            const ownerId = request.owner?.id;
            const status = request.status || 'PENDING';
            const statusClass = status.toLowerCase();
            const message = request.message || '';
            
            let actionButtons = '';
            let verificationStatus = '';
            let ratingSection = '';
            
            if (status === 'PENDING') {
                actionButtons = `<button class="btn btn-danger" onclick="cancelRequest(${request.id})">Cancel Request</button>`;
            } else if (status === 'APPROVED') {
                actionButtons = `
                    <button class="btn btn-primary" data-chat-request="${request.id}" onclick="openChatModal(${request.id}, '${ownerFullName.replace(/'/g, "\\'")}')">💬 Chat</button>
                `;
                
                if (request.requesterVerified && request.ownerVerified) {
                    verificationStatus = '<p class="verification-status verified">✓ Both parties verified - Transaction Complete</p>';
                } else if (request.requesterVerified) {
                    verificationStatus = '<p class="verification-status pending">Waiting for owner verification</p>';
                } else {
                    verificationStatus = `
                        <p class="verification-status pending">Please verify receipt</p>
                        <button class="btn btn-success" onclick="verifyReceipt(${request.id})">✓ Verify Receipt</button>
                    `;
                }
            }
            
            item.innerHTML = `
                <div class="request-header">
                    <h4>${resourceTitle}</h4>
                    <span class="status ${statusClass}">${status}</span>
                </div>
                <div class="request-meta">
                    <p><strong>Owner:</strong> <a href="#" class="owner-link" onclick="viewUserProfile(${ownerId}); return false;">${ownerFullName}</a></p>
                    <p><strong>Category:</strong> ${resourceCategory}</p>
                    ${message ? `<p><strong>Message:</strong> ${message}</p>` : ''}
                </div>
                ${verificationStatus}
                <div class="actions" id="actions-${request.id}">
                    ${actionButtons}
                </div>
            `;
            
            container.appendChild(item);
            
            // Add rating button if transaction is complete
            if (request.requesterVerified && request.ownerVerified && ownerId) {
                checkAndShowRatingButton(request.id, ownerId, ownerFullName, document.getElementById(`actions-${request.id}`));
            }
        } catch (error) {
            // Silently skip problematic requests
        }
    });
}

async function loadReceivedRequests() {
    if (!currentUser || !currentUser.id) {
        console.error('User not logged in');
        return;
    }
    
    try {
        const response = await fetchWithRetry(`${API_BASE_URL}/requests/owner/${currentUser.id}`, {
            method: 'GET'
        });
        
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        
        const data = await response.json();
        
        if (data.success) {
            displayReceivedRequests(data.data);
        } else {
            console.error('Failed to load received requests:', data.message);
        }
    } catch (error) {
        console.error('Error loading received requests:', error);
    }
}

function displayReceivedRequests(requests) {
    const container = document.getElementById('receivedRequestsContainer');
    container.innerHTML = '';
    
    if (!requests || !Array.isArray(requests)) {
        container.innerHTML = '<p style="text-align: center; color: #666;">Error loading requests</p>';
        return;
    }
    
    if (requests.length === 0) {
        container.innerHTML = '<p style="text-align: center; color: #666;">No requests received</p>';
        return;
    }
    
    // Count pending requests for badge
    var pendingCount = 0;
    requests.forEach(function(r) { if (r && r.status === 'PENDING') pendingCount++; });
    updateReceivedRequestsBadge(pendingCount);

    requests.forEach((request) => {
        try {
            if (!request) {
                return;
            }
            
            const item = document.createElement('div');
            item.className = 'request-item';
            
            const resourceTitle = request.resource?.title || 'Unknown Resource';
            const resourceCategory = request.resource?.category || 'N/A';
            const requesterFullName = request.requester?.fullName || request.requester?.username || 'Unknown';
            const requesterId = request.requester?.id;
            const status = request.status || 'PENDING';
            const statusClass = status.toLowerCase();
            const message = request.message || '';
            
            let actionButtons = '';
            let verificationStatus = '';
            
            if (status === 'PENDING') {
                actionButtons = `
                    <button class="btn btn-success" onclick="approveRequest(${request.id})">Approve</button>
                    <button class="btn btn-danger" onclick="rejectRequest(${request.id})">Reject</button>
                `;
            } else if (status === 'APPROVED') {
                actionButtons = `
                    <button class="btn btn-primary" data-chat-request="${request.id}" onclick="openChatModal(${request.id}, '${requesterFullName.replace(/'/g, "\\'")}')">💬 Chat</button>
                `;
                
                if (request.ownerVerified && request.requesterVerified) {
                    verificationStatus = '<p class="verification-status verified">✓ Both parties verified - Transaction Complete</p>';
                } else if (request.ownerVerified) {
                    verificationStatus = '<p class="verification-status pending">Waiting for requester verification</p>';
                } else {
                    verificationStatus = `
                        <p class="verification-status pending">Please verify handover</p>
                        <button class="btn btn-success" onclick="verifyHandover(${request.id})">✓ Verify Handover</button>
                    `;
                }
            }
            
            item.innerHTML = `
                <div class="request-header">
                    <h4>${resourceTitle}</h4>
                    <span class="status ${statusClass}">${status}</span>
                </div>
                <div class="request-meta">
                    <p><strong>Requester:</strong> <a href="#" class="owner-link" onclick="viewUserProfile(${requesterId}); return false;">${requesterFullName}</a></p>
                    <p><strong>Category:</strong> ${resourceCategory}</p>
                    ${message ? `<p><strong>Message:</strong> ${message}</p>` : ''}
                </div>
                ${verificationStatus}
                <div class="actions" id="recv-actions-${request.id}">
                    ${actionButtons}
                </div>
            `;
            
            container.appendChild(item);
            
            // Add rating button if transaction is complete
            if (request.ownerVerified && request.requesterVerified && requesterId) {
                checkAndShowRatingButton(request.id, requesterId, requesterFullName, document.getElementById(`recv-actions-${request.id}`));
            }
        } catch (error) {
            // Silently skip problematic requests
        }
    });
}

// ========== ADD RESOURCE ==========

let currentResourceId = null;

function showAddResourceModal() {
    document.getElementById('addResourceModal').classList.add('active');
    // Ensure the user starts at the top of the modal (important on small screens)
    const modalContent = document.querySelector('#addResourceModal .modal-content');
    if (modalContent) modalContent.scrollTop = 0;
}

function closeAddResourceModal() {
    document.getElementById('addResourceModal').classList.remove('active');
    document.getElementById('addResourceForm').reset();
    // Hide all transaction fields and reset required attributes
    document.querySelectorAll('.transaction-fields').forEach(field => {
        field.style.display = 'none';
    });
    // Reset required attributes
    document.getElementById('sellPrice').required = false;
    document.getElementById('exchangeDescription').required = false;
    document.getElementById('rentalDuration').required = false;
    document.getElementById('rentPrice').required = false;
}

// Handle transaction type change to show/hide fields
function handleTransactionTypeChange() {
    const transactionType = document.getElementById('transactionType').value;
    
    // Hide all transaction fields
    document.querySelectorAll('.transaction-fields').forEach(field => {
        field.style.display = 'none';
    });
    
    // Show relevant fields based on selection
    if (transactionType === 'SELL') {
        document.getElementById('sellFields').style.display = 'block';
        document.getElementById('sellPrice').required = true;
    } else {
        document.getElementById('sellPrice').required = false;
    }
    
    if (transactionType === 'EXCHANGE') {
        document.getElementById('exchangeFields').style.display = 'block';
        document.getElementById('exchangeDescription').required = true;
    } else {
        document.getElementById('exchangeDescription').required = false;
    }
    
    if (transactionType === 'RENT') {
        document.getElementById('rentFields').style.display = 'block';
        document.getElementById('rentalDuration').required = true;
        document.getElementById('rentPrice').required = true;
    } else {
        document.getElementById('rentalDuration').required = false;
        document.getElementById('rentPrice').required = false;
    }
    
    if (transactionType === 'DONATE') {
        document.getElementById('donateFields').style.display = 'block';
    }
}

async function handleAddResource(event) {
    event.preventDefault();
    const title = document.getElementById('resourceTitle').value;
    const description = document.getElementById('resourceDescription').value;
    const category = document.getElementById('resourceCategory').value;
    const conditionStatus = document.getElementById('resourceCondition').value;
    const txEl = document.getElementById('transactionType');
    const transactionType = txEl ? txEl.value : '';
    if (!txEl) {
        showMessage('Transaction Type field is missing on this page. Please hard refresh (Ctrl+F5).', 'error');
        return;
    }
    
    // Strict validation for transaction type
    const validTransactionTypes = ['SELL', 'EXCHANGE', 'RENT', 'DONATE'];
    if (!transactionType || !validTransactionTypes.includes(transactionType)) {
        // Bring the Transaction Type field into view so user can select it
        const txEl = document.getElementById('transactionType');
        if (txEl) {
            txEl.scrollIntoView({ behavior: 'smooth', block: 'center' });
            txEl.focus();
        }
        showMessage('Please select Transaction Type (Sell / Exchange / Rent / Donate)', 'error');
        return;
    }
    
    // Build resource object based on transaction type
    const resourceData = {
        title: title.trim(),
        description: description.trim(),
        category: category.trim(),
        conditionStatus: conditionStatus.trim(),
        transactionType: transactionType.trim()
    };
    
    // Add fields based on transaction type
    if (transactionType === 'SELL') {
        const price = parseFloat(document.getElementById('sellPrice').value);
        if (!price || price <= 0) {
            showMessage('Please enter a valid selling price', 'error');
            return;
        }
        resourceData.price = price;
    } else if (transactionType === 'EXCHANGE') {
        const exchangeDesc = document.getElementById('exchangeDescription').value.trim();
        if (!exchangeDesc) {
            showMessage('Please describe what you want in exchange', 'error');
            return;
        }
        resourceData.exchangeDescription = exchangeDesc;
    } else if (transactionType === 'RENT') {
        const rentalDuration = document.getElementById('rentalDuration').value;
        const rentPrice = parseFloat(document.getElementById('rentPrice').value);
        if (!rentalDuration) {
            showMessage('Please select rental duration', 'error');
            return;
        }
        if (!rentPrice || rentPrice <= 0) {
            showMessage('Please enter a valid rental price', 'error');
            return;
        }
        resourceData.rentalDuration = rentalDuration;
        resourceData.price = rentPrice;
    } else if (transactionType === 'DONATE') {
        // No additional fields needed for donation
        resourceData.price = null;
    }
    
    try {
        const response = await fetch(`${API_BASE_URL}/resources?ownerId=${currentUser.id}`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(resourceData)
        });
        
        const data = await response.json();
        
        if (data.success) {
            showMessage('Resource added successfully!', 'success');
            closeAddResourceModal();
            loadResources();
            loadMyResources();
            // Reset form
            event.target.reset();
        } else {
            showMessage(data.message || 'Failed to add resource', 'error');
        }
    } catch (error) {
        showMessage('Error connecting to server', 'error');
        console.error('Add resource error:', error);
    }
}

// ========== REQUEST RESOURCE ==========

function openRequestModal(resourceId) {
    currentResourceId = resourceId;
    document.getElementById('requestModal').classList.add('active');
}

function closeRequestModal() {
    document.getElementById('requestModal').classList.remove('active');
    document.getElementById('requestMessage').value = '';
    currentResourceId = null;
}

async function handleCreateRequest(event) {
    event.preventDefault();
    const message = document.getElementById('requestMessage').value;
    
    try {
        const response = await fetch(
            `${API_BASE_URL}/requests?resourceId=${currentResourceId}&requesterId=${currentUser.id}&message=${encodeURIComponent(message)}`,
            {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                }
            }
        );
        
        const data = await response.json();
        
        if (data.success) {
            showMessage('Request sent successfully!', 'success');
            closeRequestModal();
            loadResources();
            loadMyRequests();
        } else {
            showMessage(data.message || 'Failed to send request', 'error');
        }
    } catch (error) {
        showMessage('Error connecting to server', 'error');
        console.error('Create request error:', error);
    }
}

// ========== REQUEST ACTIONS ==========

async function approveRequest(requestId) {
    try {
        const response = await fetch(
            `${API_BASE_URL}/requests/${requestId}/approve?ownerId=${currentUser.id}`,
            {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json'
                }
            }
        );
        
        const data = await response.json();
        
        if (data.success) {
            showMessage('Request approved! Chat is now enabled.', 'success');
            loadResources();
            loadReceivedRequests();
        } else {
            showMessage(data.message || 'Failed to approve request', 'error');
        }
    } catch (error) {
        showMessage('Error connecting to server', 'error');
        console.error('Approve request error:', error);
    }
}

async function rejectRequest(requestId) {
    try {
        const response = await fetch(
            `${API_BASE_URL}/requests/${requestId}/reject?ownerId=${currentUser.id}`,
            {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json'
                }
            }
        );
        
        const data = await response.json();
        
        if (data.success) {
            showMessage('Request rejected', 'info');
            loadReceivedRequests();
        } else {
            showMessage(data.message || 'Failed to reject request', 'error');
        }
    } catch (error) {
        showMessage('Error connecting to server', 'error');
        console.error('Reject request error:', error);
    }
}

async function cancelRequest(requestId) {
    try {
        const response = await fetch(
            `${API_BASE_URL}/requests/${requestId}/cancel?requesterId=${currentUser.id}`,
            {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json'
                }
            }
        );
        
        const data = await response.json();
        
        if (data.success) {
            showMessage('Request cancelled', 'info');
            loadMyRequests();
            loadResources();
        } else {
            showMessage(data.message || 'Failed to cancel request', 'error');
        }
    } catch (error) {
        showMessage('Error connecting to server', 'error');
        console.error('Cancel request error:', error);
    }
}

// ========== TABS ==========

function showTab(tabName) {
    // Hide all tab contents
    document.querySelectorAll('.tab-content').forEach(tab => {
        tab.classList.remove('active');
    });
    
    // Remove active class from all tab buttons
    document.querySelectorAll('.tabs .tab-btn').forEach(btn => {
        btn.classList.remove('active');
    });
    
    // Show selected tab
    document.getElementById(tabName).classList.add('active');
    
    // Add active class to clicked button
    event.target.classList.add('active');
    
    // Reload data if needed
    if (tabName === 'myResources') {
        loadMyResources();
    } else if (tabName === 'myRequests') {
        loadMyRequests();
    } else if (tabName === 'receivedRequests') {
        loadReceivedRequests();
    }
}

// ========== CHAT ==========

let currentChatRequestId = null;
let chatInterval = null;

function openChatModal(requestId, otherUserName) {
    currentChatRequestId = requestId;
    document.getElementById('chatTitle').textContent = `Chat with ${otherUserName}`;
    document.getElementById('chatModal').classList.add('active');
    loadChatMessages();
    
    // Backend automatically marks messages as read when we call getMessages
    // Just update badges after opening
    setTimeout(updateChatBadges, 500);
    
    // Auto-refresh messages every 2 seconds
    chatInterval = setInterval(function() {
        loadChatMessages();
        // Update badges periodically
        updateChatBadges();
    }, 2000);
}

function closeChatModal() {
    document.getElementById('chatModal').classList.remove('active');
    currentChatRequestId = null;
    if (chatInterval) {
        clearInterval(chatInterval);
        chatInterval = null;
    }
    
    // Update all badges after closing
    setTimeout(updateChatBadges, 100);
}

async function loadChatMessages() {
    if (!currentChatRequestId) return;
    
    try {
        const response = await fetch(
            `${API_BASE_URL}/chat/messages?requestId=${currentChatRequestId}&userId=${currentUser.id}`
        );
        const data = await response.json();
        
        if (data.success) {
            displayChatMessages(data.data);
        }
    } catch (error) {
        console.error('Error loading messages:', error);
    }
}

function displayChatMessages(messages) {
    const container = document.getElementById('chatMessages');
    container.innerHTML = '';
    
    messages.forEach(message => {
        const messageDiv = document.createElement('div');
        const isOwnMessage = message.sender.id === currentUser.id;
        messageDiv.className = `chat-message ${isOwnMessage ? 'own' : 'other'}`;
        
        const time = new Date(message.createdAt).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'});
        
        messageDiv.innerHTML = `
            <div class="message-header">
                <strong>${message.sender.fullName}</strong>
                <span class="message-time">${time}</span>
            </div>
            <div class="message-content">${message.content}</div>
        `;
        
        container.appendChild(messageDiv);
    });
    
    // Scroll to bottom
    container.scrollTop = container.scrollHeight;
}

function handleChatKeyPress(event) {
    if (event.key === 'Enter') {
        sendChatMessage();
    }
}

async function sendChatMessage() {
    const input = document.getElementById('chatInput');
    const content = input.value.trim();
    
    if (!content || !currentChatRequestId) return;
    
    try {
        const response = await fetch(
            `${API_BASE_URL}/chat/send?requestId=${currentChatRequestId}&senderId=${currentUser.id}&content=${encodeURIComponent(content)}`,
            {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                }
            }
        );
        
        const data = await response.json();
        
        if (data.success) {
            input.value = '';
            loadChatMessages();
        } else {
            showMessage(data.message || 'Failed to send message', 'error');
        }
    } catch (error) {
        showMessage('Error sending message', 'error');
        console.error('Send message error:', error);
    }
}

// ========== VERIFICATION ==========

async function verifyHandover(requestId) {
    if (!confirm('Confirm that you have handed over the resource?')) {
        return;
    }
    
    try {
        const response = await fetch(
            `${API_BASE_URL}/requests/${requestId}/verify-handover?ownerId=${currentUser.id}`,
            {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json'
                }
            }
        );
        
        const data = await response.json();
        
        if (data.success) {
            showMessage('Handover verified successfully!', 'success');
            loadReceivedRequests();
            loadResources();
        } else {
            showMessage(data.message || 'Failed to verify handover', 'error');
        }
    } catch (error) {
        showMessage('Error connecting to server', 'error');
        console.error('Verify handover error:', error);
    }
}

async function verifyReceipt(requestId) {
    if (!confirm('Confirm that you have received the resource?')) {
        return;
    }
    
    try {
        const response = await fetch(
            `${API_BASE_URL}/requests/${requestId}/verify-receipt?requesterId=${currentUser.id}`,
            {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json'
                }
            }
        );
        
        const data = await response.json();
        
        if (data.success) {
            showMessage('Receipt verified successfully!', 'success');
            loadMyRequests();
            loadResources();
        } else {
            showMessage(data.message || 'Failed to verify receipt', 'error');
        }
    } catch (error) {
        showMessage('Error connecting to server', 'error');
        console.error('Verify receipt error:', error);
    }
}

function closeVerificationModal() {
    document.getElementById('verificationModal').classList.remove('active');
}

// ========== UTILITY ==========

function showMessage(text, type = 'info') {
    const messageDiv = document.getElementById('message');
    messageDiv.textContent = text;
    messageDiv.className = `message ${type} show`;
    
    setTimeout(() => {
        messageDiv.classList.remove('show');
    }, 3000);
}

// Close modals when clicking outside
window.onclick = function(event) {
    const addModal = document.getElementById('addResourceModal');
    const requestModal = document.getElementById('requestModal');
    const chatModal = document.getElementById('chatModal');
    const verificationModal = document.getElementById('verificationModal');
    const profileModal = document.getElementById('profileModal');
    const viewProfileModal = document.getElementById('viewProfileModal');
    const ratingModal = document.getElementById('ratingModal');
    
    if (event.target === addModal) {
        closeAddResourceModal();
    }
    if (event.target === requestModal) {
        closeRequestModal();
    }
    if (event.target === chatModal) {
        closeChatModal();
    }
    if (event.target === verificationModal) {
        closeVerificationModal();
    }
    if (event.target === profileModal) {
        closeProfileModal();
    }
    if (event.target === viewProfileModal) {
        closeViewProfileModal();
    }
    if (event.target === ratingModal) {
        closeRatingModal();
    }
}

// ========== PROFILE ==========

async function showProfileModal() {
    document.getElementById('profileModal').classList.add('active');
    await loadProfile();
    await loadUserRatings(currentUser.id, 'profileRatings');
}

function closeProfileModal() {
    document.getElementById('profileModal').classList.remove('active');
}

async function loadProfile() {
    try {
        const response = await fetch(`${API_BASE_URL}/users/${currentUser.id}`);
        const data = await response.json();
        
        if (data.success) {
            const user = data.data;
            
            // Update profile header
            document.getElementById('profileAvatar').textContent = (user.fullName || 'U').charAt(0).toUpperCase();
            document.getElementById('profileFullName').textContent = user.fullName || 'User';
            document.getElementById('profileUsername').textContent = '@' + user.username;
            
            // Update rating display
            const avgRating = user.averageRating || 0;
            const totalRatings = user.totalRatings || 0;
            document.getElementById('profileRating').innerHTML = 
                `${getStarRating(avgRating)} <span class="rating-text">${avgRating.toFixed(1)} (${totalRatings} ratings)</span>`;
            
            // Update navbar rating
            updateNavbarRating(avgRating, totalRatings);
            
            // Fill form
            document.getElementById('profileFullNameInput').value = user.fullName || '';
            document.getElementById('profilePhone').value = user.phone || '';
            document.getElementById('profileDepartment').value = user.department || '';
            document.getElementById('profileYear').value = user.year || '';
            document.getElementById('profileBio').value = user.bio || '';
            
            // Update currentUser in localStorage
            currentUser = { ...currentUser, ...user };
            localStorage.setItem('currentUser', JSON.stringify(currentUser));
        }
    } catch (error) {
        console.error('Error loading profile:', error);
        showMessage('Error loading profile', 'error');
    }
}

function updateNavbarRating(avgRating, totalRatings) {
    const ratingBadge = document.getElementById('userRating');
    if (totalRatings > 0) {
        ratingBadge.innerHTML = `⭐ ${avgRating.toFixed(1)}`;
        ratingBadge.style.display = 'inline-block';
    } else {
        ratingBadge.style.display = 'none';
    }
}

async function handleUpdateProfile(event) {
    event.preventDefault();
    
    const profileData = {
        fullName: document.getElementById('profileFullNameInput').value,
        phone: document.getElementById('profilePhone').value,
        department: document.getElementById('profileDepartment').value,
        year: document.getElementById('profileYear').value,
        bio: document.getElementById('profileBio').value
    };
    
    try {
        const response = await fetch(`${API_BASE_URL}/users/${currentUser.id}/profile`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(profileData)
        });
        
        const data = await response.json();
        
        if (data.success) {
            showMessage('Profile updated successfully!', 'success');
            
            // Update currentUser
            currentUser = { ...currentUser, ...data.data };
            localStorage.setItem('currentUser', JSON.stringify(currentUser));
            
            // Update UI
            document.getElementById('userName').textContent = currentUser.fullName || currentUser.username;
            document.getElementById('profileFullName').textContent = currentUser.fullName;
            document.getElementById('profileAvatar').textContent = (currentUser.fullName || 'U').charAt(0).toUpperCase();
        } else {
            showMessage(data.message || 'Failed to update profile', 'error');
        }
    } catch (error) {
        console.error('Error updating profile:', error);
        showMessage('Error updating profile', 'error');
    }
}

// ========== VIEW OTHER USER PROFILE ==========

let viewingUserId = null;

async function viewUserProfile(userId) {
    viewingUserId = userId;
    document.getElementById('viewProfileModal').classList.add('active');
    await loadUserProfile(userId);
    await loadUserRatings(userId, 'viewProfileRatings');
}

function closeViewProfileModal() {
    document.getElementById('viewProfileModal').classList.remove('active');
    viewingUserId = null;
}

async function loadUserProfile(userId) {
    try {
        const response = await fetch(`${API_BASE_URL}/users/${userId}`);
        const data = await response.json();
        
        if (data.success) {
            const user = data.data;
            
            document.getElementById('viewProfileAvatar').textContent = (user.fullName || 'U').charAt(0).toUpperCase();
            document.getElementById('viewProfileFullName').textContent = user.fullName || 'User';
            document.getElementById('viewProfileUsername').textContent = '@' + user.username;
            
            const avgRating = user.averageRating || 0;
            const totalRatings = user.totalRatings || 0;
            document.getElementById('viewProfileRating').innerHTML = 
                `${getStarRating(avgRating)} <span class="rating-text">${avgRating.toFixed(1)} (${totalRatings} ratings)</span>`;
            
            document.getElementById('viewProfileDepartment').textContent = user.department || 'Not specified';
            document.getElementById('viewProfileYear').textContent = user.year || 'Not specified';
            document.getElementById('viewProfileBio').textContent = user.bio || 'No bio provided';
            
            if (user.createdAt) {
                const date = new Date(user.createdAt);
                document.getElementById('viewProfileMemberSince').textContent = date.toLocaleDateString('en-IN', {
                    year: 'numeric',
                    month: 'long'
                });
            }
        }
    } catch (error) {
        console.error('Error loading user profile:', error);
    }
}

async function loadUserRatings(userId, containerId) {
    try {
        const response = await fetch(`${API_BASE_URL}/ratings/user/${userId}`);
        const data = await response.json();
        
        const container = document.getElementById(containerId);
        container.innerHTML = '';
        
        if (data.success && data.data && data.data.length > 0) {
            data.data.slice(0, 5).forEach(rating => {
                const ratingItem = document.createElement('div');
                ratingItem.className = 'rating-item';
                const date = new Date(rating.createdAt);
                ratingItem.innerHTML = `
                    <div class="rating-header">
                        <span class="rating-stars">${getStarRating(rating.score)}</span>
                        <span class="rating-date">${date.toLocaleDateString()}</span>
                    </div>
                    <p class="rating-from">From: ${rating.rater.fullName}</p>
                    ${rating.comment ? `<p class="rating-comment">"${rating.comment}"</p>` : ''}
                    <p class="rating-resource">Transaction: ${rating.request.resourceTitle}</p>
                `;
                container.appendChild(ratingItem);
            });
        } else {
            container.innerHTML = '<p style="color: #666; text-align: center;">No ratings yet</p>';
        }
    } catch (error) {
        console.error('Error loading ratings:', error);
    }
}

function getStarRating(score) {
    const fullStars = Math.floor(score);
    const halfStar = score % 1 >= 0.5 ? 1 : 0;
    const emptyStars = 5 - fullStars - halfStar;
    
    return '★'.repeat(fullStars) + (halfStar ? '☆' : '') + '☆'.repeat(emptyStars);
}

// ========== RATING ==========

let ratingRequestId = null;
let ratingUserId = null;
let ratingUserName = '';
let selectedRating = 0;

function openRatingModal(requestId, userId, userName) {
    ratingRequestId = requestId;
    ratingUserId = userId;
    ratingUserName = userName;
    selectedRating = 0;
    
    document.getElementById('ratingUserName').innerHTML = `Rate your experience with <strong>${userName}</strong>`;
    document.getElementById('ratingScore').value = 0;
    document.getElementById('ratingComment').value = '';
    
    // Reset stars
    document.querySelectorAll('#starRating .star').forEach(star => {
        star.classList.remove('active');
    });
    
    document.getElementById('ratingModal').classList.add('active');
    
    // Setup star click handlers
    setupStarRating();
}

function closeRatingModal() {
    document.getElementById('ratingModal').classList.remove('active');
    ratingRequestId = null;
    ratingUserId = null;
    ratingUserName = '';
    selectedRating = 0;
}

function setupStarRating() {
    const stars = document.querySelectorAll('#starRating .star');
    stars.forEach(star => {
        star.onclick = function() {
            selectedRating = parseInt(this.dataset.value);
            document.getElementById('ratingScore').value = selectedRating;
            
            stars.forEach((s, index) => {
                if (index < selectedRating) {
                    s.classList.add('active');
                } else {
                    s.classList.remove('active');
                }
            });
        };
        
        star.onmouseenter = function() {
            const val = parseInt(this.dataset.value);
            stars.forEach((s, index) => {
                if (index < val) {
                    s.classList.add('hover');
                } else {
                    s.classList.remove('hover');
                }
            });
        };
        
        star.onmouseleave = function() {
            stars.forEach(s => s.classList.remove('hover'));
        };
    });
}

async function handleSubmitRating(event) {
    event.preventDefault();
    
    if (selectedRating < 1 || selectedRating > 5) {
        showMessage('Please select a rating', 'error');
        return;
    }
    
    const comment = document.getElementById('ratingComment').value;
    
    try {
        const response = await fetch(
            `${API_BASE_URL}/ratings?requestId=${ratingRequestId}&raterId=${currentUser.id}&ratedUserId=${ratingUserId}&score=${selectedRating}&comment=${encodeURIComponent(comment)}`,
            {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                }
            }
        );
        
        const data = await response.json();
        
        if (data.success) {
            showMessage('Rating submitted successfully!', 'success');
            closeRatingModal();
            loadMyRequests();
            loadReceivedRequests();
        } else {
            showMessage(data.message || 'Failed to submit rating', 'error');
        }
    } catch (error) {
        console.error('Error submitting rating:', error);
        showMessage('Error submitting rating', 'error');
    }
}

async function checkAndShowRatingButton(requestId, otherUserId, otherUserName, container) {
    try {
        const response = await fetch(
            `${API_BASE_URL}/ratings/check?requestId=${requestId}&raterId=${currentUser.id}`
        );
        const data = await response.json();
        
        if (data.success && !data.data.hasRated) {
            const rateBtn = document.createElement('button');
            rateBtn.className = 'btn btn-warning';
            rateBtn.innerHTML = '⭐ Rate';
            rateBtn.onclick = () => openRatingModal(requestId, otherUserId, otherUserName);
            container.appendChild(rateBtn);
        }
    } catch (error) {
        console.error('Error checking rating status:', error);
    }
}

// ========== BADGE UPDATES ==========

function updateReceivedRequestsBadge(count) {
    var badge = document.getElementById('receivedBadge');
    if (!badge) return;
    if (count > 0) {
        badge.textContent = count;
        badge.classList.add('visible');
    } else {
        badge.textContent = '';
        badge.classList.remove('visible');
    }
}

async function updateChatBadges() {
    if (!currentUser) return;
    
    try {
        console.log('Updating chat badges for user:', currentUser.id);
        
        // Use the efficient all-unread-counts API
        var response = await fetch(
            API_BASE_URL + '/chat/all-unread-counts?userId=' + currentUser.id
        );
        var data = await response.json();
        
        console.log('Unread counts response:', data);
        
        if (data.success && data.data) {
            var allCounts = data.data;
            var chatButtons = document.querySelectorAll('[data-chat-request]');
            
            console.log('Found chat buttons:', chatButtons.length);
            console.log('Unread counts:', allCounts);
            
            for (var i = 0; i < chatButtons.length; i++) {
                var btn = chatButtons[i];
                var requestId = btn.getAttribute('data-chat-request');
                var unreadCount = allCounts[requestId] || 0;
                
                console.log(`Button ${i}: Request ID ${requestId}, Unread: ${unreadCount}`);
                
                // Remove existing badge if any
                var existingBadge = btn.querySelector('.chat-badge');
                if (existingBadge) {
                    existingBadge.remove();
                }
                
                // Add badge if there are unread messages
                if (unreadCount > 0) {
                    var badge = document.createElement('span');
                    badge.className = 'chat-badge';
                    badge.textContent = unreadCount > 99 ? '99+' : unreadCount.toString();
                    btn.appendChild(badge);
                    
                    console.log(`Added badge with count: ${unreadCount}`);
                    
                    // Add animation
                    badge.style.animation = 'none';
                    setTimeout(function() {
                        badge.style.animation = '';
                    }, 10);
                }
            }
        }
    } catch (e) {
        console.log('Error updating chat badges:', e);
    }
}

// Periodic badge updates (WhatsApp-style)
let badgeUpdateInterval = null;

function startPeriodicBadgeUpdates() {
    // Clear any existing interval
    if (badgeUpdateInterval) {
        clearInterval(badgeUpdateInterval);
    }
    
    // Update badges every 5 seconds
    badgeUpdateInterval = setInterval(function() {
        updateChatBadges();
    }, 5000);
}

function stopPeriodicBadgeUpdates() {
    if (badgeUpdateInterval) {
        clearInterval(badgeUpdateInterval);
        badgeUpdateInterval = null;
    }
}

// Clean up on page unload
window.addEventListener('beforeunload', function() {
    stopPeriodicBadgeUpdates();
});

// Manual test function - call this from browser console
function testChatBadges() {
    console.log('=== Testing Chat Badges ===');
    console.log('Current user:', currentUser);
    
    var chatButtons = document.querySelectorAll('[data-chat-request]');
    console.log('Chat buttons found:', chatButtons.length);
    
    for (var i = 0; i < chatButtons.length; i++) {
        var btn = chatButtons[i];
        console.log(`Button ${i}:`, {
            requestId: btn.getAttribute('data-chat-request'),
            text: btn.textContent,
            hasBadge: btn.querySelector('.chat-badge') !== null
        });
    }
    
    // Force update badges
    updateChatBadges();
}
