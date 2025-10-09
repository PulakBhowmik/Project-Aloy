// Global variable to track if user has an existing booking
let userHasBooking = false;
let userBookingDetails = null;

// Wait for DOM to load
document.addEventListener('DOMContentLoaded', function() {
    // Check tenant booking status first
    checkTenantBookingStatus();

    // Load apartments when page loads
    loadApartments();

    // Set up search form
    const searchForm = document.getElementById('searchForm');
    if (searchForm) {
        searchForm.addEventListener('submit', function(e) {
            e.preventDefault();
            performSearch();
        });
    }

    // Load stats
    loadStats();
});

// Check if current tenant already has a booking
function checkTenantBookingStatus() {
    const user = JSON.parse(localStorage.getItem('user'));
    if (!user || !user.userId) {
        console.log('[INFO] No user logged in, skipping booking status check');
        return;
    }

    fetch(`/api/tenants/${user.userId}/booking-status`)
        .then(response => response.json())
        .then(data => {
            if (data.hasBooking) {
                userHasBooking = true;
                userBookingDetails = data;
                console.log('[INFO] User already has a booking:', data);
                // Show vacate button for users with active bookings
                showVacateButton(data);
            } else {
                userHasBooking = false;
                userBookingDetails = null;
                console.log('[INFO] User has no existing booking');
            }
        })
        .catch(error => {
            console.error('[ERROR] Failed to check tenant booking status:', error);
        });
    
    // Also check if tenant is in a roommate group
    checkTenantGroupStatus();
}

// Show vacate button for tenants with active bookings
function showVacateButton(bookingData) {
    const container = document.getElementById('vacateButtonContainer');
    if (!container) return;
    
    const html = `
        <div class="card mt-4 border-warning">
            <div class="card-header bg-warning text-dark">
                <h5><i class="bi bi-house-door"></i> Your Current Booking</h5>
            </div>
            <div class="card-body">
                <h6>${bookingData.apartmentTitle || 'Apartment'}</h6>
                <p><strong>Monthly Rent:</strong> ৳${bookingData.monthlyRent}</p>
                <p><strong>Transaction ID:</strong> ${bookingData.transactionId}</p>
                <button class="btn btn-danger" onclick="showVacateModal()">
                    <i class="bi bi-box-arrow-right"></i> Vacate Apartment
                </button>
            </div>
        </div>
    `;
    container.innerHTML = html;
}

// Show vacate modal
window.showVacateModal = function() {
    const user = JSON.parse(localStorage.getItem('user'));
    if (!userBookingDetails) {
        alert('No active booking found');
        return;
    }
    
    const today = new Date().toISOString().split('T')[0];
    
    const html = `
        <div class="modal fade" id="vacateModal" tabindex="-1" aria-hidden="true">
            <div class="modal-dialog">
                <div class="modal-content">
                    <div class="modal-header bg-danger text-white">
                        <h5 class="modal-title">Vacate Apartment</h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                    </div>
                    <div class="modal-body">
                        <p><strong>Apartment:</strong> ${userBookingDetails.apartmentTitle || 'Your apartment'}</p>
                        <p><strong>Monthly Rent:</strong> ৳${userBookingDetails.monthlyRent}</p>
                        <hr>
                        <form id="vacateForm">
                            <div class="mb-3">
                                <label for="vacateDate" class="form-label">Vacate Date</label>
                                <input type="date" class="form-control" id="vacateDate" 
                                       min="${today}" required>
                                <small class="form-text text-muted">
                                    Select the date you plan to leave the apartment
                                </small>
                            </div>
                            <div class="alert alert-warning">
                                <strong>Note:</strong> Once you vacate, the apartment will be available for others to book.
                            </div>
                            <button type="submit" class="btn btn-danger w-100">Confirm Vacate</button>
                        </form>
                        <div id="vacateMessage" class="mt-3"></div>
                    </div>
                </div>
            </div>
        </div>
    `;
    
    document.body.insertAdjacentHTML('beforeend', html);
    const modal = new bootstrap.Modal(document.getElementById('vacateModal'));
    modal.show();
    
    // Handle form submission
    document.getElementById('vacateForm').addEventListener('submit', function(e) {
        e.preventDefault();
        const vacateDate = document.getElementById('vacateDate').value;
        
        fetch('/api/vacate', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                tenantId: user.userId,
                apartmentId: userBookingDetails.apartmentId,
                vacateDate: vacateDate
            })
        })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                document.getElementById('vacateMessage').innerHTML = 
                    `<div class="alert alert-success">
                        <strong>Success!</strong> ${data.message}
                        <br><small>Vacate Date: ${data.vacateDate}</small>
                        <br><br>
                        <button class="btn btn-primary" onclick="showReviewModal(${userBookingDetails.apartmentId})">
                            <i class="bi bi-star-fill"></i> Leave a Review
                        </button>
                        <button class="btn btn-secondary" onclick="window.location.reload()">Skip</button>
                    </div>`;
                
                // Clear booking status
                userHasBooking = false;
                const apartmentId = userBookingDetails.apartmentId;
                userBookingDetails = null;
            } else {
                document.getElementById('vacateMessage').innerHTML = 
                    `<div class="alert alert-danger">
                        <strong>Error:</strong> ${data.error || 'Failed to vacate apartment'}
                    </div>`;
            }
        })
        .catch(error => {
            console.error('Error:', error);
            document.getElementById('vacateMessage').innerHTML = 
                `<div class="alert alert-danger">
                    <strong>Error:</strong> Failed to process vacate request
                </div>`;
        });
    });
}

// Check tenant's roommate group status
function checkTenantGroupStatus() {
    const user = JSON.parse(localStorage.getItem('user'));
    if (!user || !user.userId || user.role.toUpperCase() !== 'TENANT') {
        return;
    }

    fetch(`/api/groups/tenant/${user.userId}/status`)
        .then(response => response.json())
        .then(data => {
            if (data.inGroup) {
                console.log('[INFO] User is in a roommate group:', data);
                showGroupStatusBox(data);
            } else {
                console.log('[INFO] User is not in any active group');
            }
        })
        .catch(error => {
            console.error('[ERROR] Failed to check tenant group status:', error);
        });
}

// Show group status box for tenants in a group
function showGroupStatusBox(groupData) {
    const container = document.getElementById('vacateButtonContainer');
    if (!container) return;
    
    const statusColor = groupData.status === 'READY' ? 'success' : 'info';
    const statusIcon = groupData.status === 'READY' ? 'check-circle-fill' : 'hourglass-split';
    const memberProgressPercent = (groupData.memberCount / groupData.maxMembers) * 100;
    
    const html = `
        <div class="card mt-4 border-${statusColor}">
            <div class="card-header bg-${statusColor} text-white">
                <h5><i class="bi bi-people-fill"></i> Your Roommate Group</h5>
            </div>
            <div class="card-body">
                <h6 class="mb-3">${groupData.apartmentTitle}</h6>
                
                <div class="mb-3">
                    <div class="d-flex justify-content-between align-items-center mb-1">
                        <strong>Group Status:</strong>
                        <span class="badge bg-${statusColor}">
                            <i class="bi bi-${statusIcon}"></i> ${groupData.status}
                        </span>
                    </div>
                    <small class="text-muted">
                        ${groupData.status === 'READY' ? 'Your group is ready to book!' : 'Waiting for more members...'}
                    </small>
                </div>
                
                <div class="mb-3">
                    <div class="d-flex justify-content-between mb-1">
                        <strong>Members:</strong>
                        <span>${groupData.memberCount}/${groupData.maxMembers}</span>
                    </div>
                    <div class="progress mb-2" style="height: 20px;">
                        <div class="progress-bar bg-${statusColor}" role="progressbar" 
                             style="width: ${memberProgressPercent}%;" 
                             aria-valuenow="${groupData.memberCount}" 
                             aria-valuemin="0" 
                             aria-valuemax="${groupData.maxMembers}">
                            ${groupData.memberCount}/${groupData.maxMembers}
                        </div>
                    </div>
                    <small class="text-muted">
                        ${groupData.memberNames.join(', ')}
                    </small>
                </div>
                
                <div class="mb-3">
                    <strong>Share with Friends:</strong>
                    <div class="input-group input-group-sm mt-2">
                        <input type="text" class="form-control" 
                               id="groupInviteLink_${groupData.groupId}" 
                               value="${window.location.origin}/join-group?code=${groupData.inviteCode}" 
                               readonly>
                        <button class="btn btn-outline-secondary" 
                                onclick="copyGroupInviteLink(${groupData.groupId}, '${groupData.inviteCode}')">
                            <i class="bi bi-clipboard"></i> Copy Link
                        </button>
                    </div>
                    <small class="text-muted">
                        <i class="bi bi-info-circle"></i> Anyone who clicks this link will be able to join your group
                    </small>
                </div>
                
                <div class="d-flex gap-2">
                    <button class="btn btn-outline-primary btn-sm" 
                            onclick="showApartmentDetailsModal(${groupData.apartmentId})">
                        <i class="bi bi-eye"></i> View Apartment
                    </button>
                    <button class="btn btn-outline-danger btn-sm" 
                            onclick="confirmLeaveGroup(${groupData.groupId})">
                        <i class="bi bi-box-arrow-right"></i> Leave Group
                    </button>
                </div>
            </div>
        </div>
    `;
    
    // Insert before or after existing content
    if (container.innerHTML.trim() === '') {
        container.innerHTML = html;
    } else {
        container.innerHTML += html;
    }
}

// Copy group invite link to clipboard
window.copyGroupInviteLink = function(groupId, inviteCode) {
    const link = window.location.origin + '/join-group?code=' + inviteCode;
    navigator.clipboard.writeText(link).then(() => {
        alert(`Invite link copied to clipboard!\n\nShare this link with your friends to join the group.`);
    }).catch(err => {
        console.error('Failed to copy:', err);
        // Fallback: select the text
        const input = document.getElementById('groupInviteLink_' + groupId);
        if (input) {
            input.select();
            alert('Link selected. Press Ctrl+C to copy.');
        } else {
            alert('Failed to copy link. Link: ' + link);
        }
    });
}

// Confirm leave group
window.confirmLeaveGroup = function(groupId) {
    if (!confirm('Are you sure you want to leave this group? This action cannot be undone.')) {
        return;
    }
    
    const user = JSON.parse(localStorage.getItem('user'));
    
    fetch(`/api/groups/${groupId}/leave`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ tenantId: user.userId })
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            alert(data.message);
            location.reload();
        } else {
            alert('Error: ' + data.message);
        }
    })
    .catch(error => {
        console.error('Error leaving group:', error);
        alert('Failed to leave group');
    });
}

// Function removed - no longer showing warning banner on homepage

// Load all apartments
function loadApartments() {
    const container = document.getElementById('apartmentsContainer');
    if (!container) return;

    // Show loading state
    container.innerHTML = '<div class="loading">Loading apartments...</div>';

    // Fetch apartments from API with cache-busting to ensure fresh data
    fetch('/api/apartments?nocache=' + Date.now(), {
        cache: 'no-store',
        headers: {
            'Cache-Control': 'no-cache, no-store, must-revalidate',
            'Pragma': 'no-cache'
        }
    })
        .then(response => response.json())
        .then(apartments => {
            displayApartments(apartments);
        })
        .catch(error => {
            console.error('Error loading apartments:', error);
            container.innerHTML = '<div class="alert alert-danger">Error loading apartments. Please try again later.</div>';
        });
}

// Perform search
function performSearch() {
    const district = document.getElementById('district')?.value || '';
    const minPrice = document.getElementById('minPrice')?.value || '';
    const maxPrice = document.getElementById('maxPrice')?.value || '';

    let url = '/api/apartments/search';
    const params = [];

    if (district) params.push(`district=${encodeURIComponent(district)}`);
    if (minPrice) params.push(`minPrice=${minPrice}`);
    if (maxPrice) params.push(`maxPrice=${maxPrice}`);

    // Add cache-busting parameter
    params.push(`nocache=${Date.now()}`);

    if (params.length > 0) {
        url += '?' + params.join('&');
    }

    const container = document.getElementById('apartmentsContainer');
    if (!container) return;

    container.innerHTML = '<div class="loading">Searching apartments...</div>';

    fetch(url, {
        cache: 'no-store',
        headers: {
            'Cache-Control': 'no-cache, no-store, must-revalidate',
            'Pragma': 'no-cache'
        }
    })
        .then(response => response.json())
        .then(apartments => {
            displayApartments(apartments);
        })
        .catch(error => {
            console.error('Error searching apartments:', error);
            container.innerHTML = '<div class="alert alert-danger">Error searching apartments. Please try again later.</div>';
        });
}

// Display apartments
function displayApartments(apartments) {
    const container = document.getElementById('apartmentsContainer');
    if (!container) return;

    if (apartments.length === 0) {
        container.innerHTML = '<div class="no-results"><h3>No apartments found</h3><p>Try adjusting your search criteria.</p></div>';
        return;
    }

    let html = '<div class="row">';
    apartments.forEach(apartment => {
        // Check if apartment is booked (standardized: only BOOKED or AVAILABLE)
        const statusUpper = (apartment.status || '').toUpperCase();
        const isBooked = apartment.booked === true || apartment.booked === 1 || 
                        statusUpper === 'BOOKED';
        const statusBadge = isBooked ? '<span class="badge bg-danger">BOOKED</span>' : '<span class="badge bg-success">AVAILABLE</span>';
        
        html += `
        <div class="col-md-6 col-lg-4 mb-4">
            <div class="card apartment-card h-100">
                <img src="https://placehold.co/600x400/4f46e5/ffffff?text=${encodeURIComponent(apartment.title)}" class="card-img-top" alt="${apartment.title}">
                <div class="card-body d-flex flex-column">
                    <h5 class="card-title mb-3">${apartment.title}</h5>
                    <div class="d-flex justify-content-between align-items-center mb-2">
                        <div class="price-tag">৳${apartment.monthlyRate}/month</div>
                        ${statusBadge}
                    </div>
                    <p class="card-text">${apartment.description || 'No description available'}</p>
                    <div class="mt-auto">
                        <p class="mb-1"><strong>Location:</strong> ${apartment.district || 'N/A'}</p>
                        <p class="mb-1"><strong>Available from:</strong> ${formatDate(apartment.availability) || 'N/A'}</p>
                        <button class="btn btn-primary" onclick="showApartmentDetailsModal(${apartment.apartmentId})">View Details</button>
                    </div>
                </div>
            </div>
        </div>`;
    });
    html += '</div>';

    container.innerHTML = html;
}

// New: calculateStats is called by index.html after a payment-refresh fetch
function calculateStats(apartments) {
    try {
        if (!Array.isArray(apartments)) return;
        // Total apartments
        const total = apartments.length;
        const totalEl = document.getElementById('totalApartments');
        if (totalEl) totalEl.textContent = total;

        // Average price
        const avgEl = document.getElementById('avgPrice');
        if (avgEl) {
            if (total > 0) {
                const avgPrice = apartments.reduce((sum, apt) => sum + (apt.monthlyRate || 0), 0) / total;
                avgEl.textContent = avgPrice.toFixed(2);
            } else {
                avgEl.textContent = '0';
            }
        }

        // Most popular district
        const popularEl = document.getElementById('popularDistrict');
        if (popularEl) {
            const districtCount = {};
            apartments.forEach(apt => {
                if (apt.district) {
                    districtCount[apt.district] = (districtCount[apt.district] || 0) + 1;
                }
            });
            let popular = '-';
            let max = 0;
            Object.entries(districtCount).forEach(([d, c]) => {
                if (c > max) { max = c; popular = d; }
            });
            popularEl.textContent = popular;
        }
    } catch (e) {
        console.error('calculateStats error:', e);
    }
}

// Reset search
function resetSearch() {
    document.getElementById('district').value = '';
    document.getElementById('minPrice').value = '';
    document.getElementById('maxPrice').value = '';
    loadApartments();
}

// Load stats
function loadStats() {
    fetch('/api/apartments')
        .then(response => response.json())
        .then(apartments => {
            // Total apartments
            document.getElementById('totalApartments').textContent = apartments.length;

            // Average price
            if (apartments.length > 0) {
                const avgPrice = apartments.reduce((sum, apt) => sum + (apt.monthlyRate || 0), 0) / apartments.length;
                document.getElementById('avgPrice').textContent = avgPrice.toFixed(2);
            }

            // Most popular district
            if (apartments.length > 0) {
                const districtCount = {};
                apartments.forEach(apt => {
                    if (apt.district) {
                        districtCount[apt.district] = (districtCount[apt.district] || 0) + 1;
                    }
                });

                let popularDistrict = '-';
                let maxCount = 0;
                for (const [district, count] of Object.entries(districtCount)) {
                    if (count > maxCount) {
                        maxCount = count;
                        popularDistrict = district;
                    }
                }
                document.getElementById('popularDistrict').textContent = popularDistrict;
            }
        })
        .catch(error => {
            console.error('Error loading stats:', error);
        });
}

// Format date
function formatDate(dateString) {
    if (!dateString) return 'N/A';
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', {
        year: 'numeric',
        month: 'long',
        day: 'numeric'
    });
}

// Apartment details modal logic
window.showApartmentDetailsModal = function(apartmentId) {
    fetch('/api/apartments/' + apartmentId)
        .then(function(response) { return response.json(); })
        .then(function(apartment) {
            var html = '<img src="https://placehold.co/800x400/4f46e5/ffffff?text=' + encodeURIComponent(apartment.title) + '" class="img-fluid mb-3" alt="' + apartment.title + '">' +
                '<h3>' + apartment.title + '</h3>' +
                '<p><strong>Description:</strong> ' + (apartment.description || 'No description available') + '</p>' +
                '<ul class="list-group mb-3">' +
                    '<li class="list-group-item"><strong>Location:</strong> ' + (apartment.address || 'N/A') + '</li>' +
                    '<li class="list-group-item"><strong>District:</strong> ' + (apartment.district || 'N/A') + '</li>' +
                    '<li class="list-group-item"><strong>Available from:</strong> ' + formatDate(apartment.availability) + '</li>' +
                    '<li class="list-group-item"><strong>Suitable for:</strong> ' + (apartment.allowedFor === 'solo' ? 'Solo' : (apartment.allowedFor === 'group' ? 'Group' : (apartment.allowedFor === 'both' ? 'Both' : 'Everyone'))) + '</li>' +
                    '<li class="list-group-item"><strong>Monthly Rate:</strong> ৳' + apartment.monthlyRate + '</li>' +
                '</ul>' +
                '<p><strong>Owner ID:</strong> ' + (apartment.ownerId || 'N/A') + '</p>';
            
            // Check if user already has a booking - disable buttons if they do
            if (userHasBooking) {
                html += '<div class="alert alert-warning">' +
                    '<strong><i class="bi bi-exclamation-triangle"></i> You already have an active booking!</strong><br>' +
                    'You can only book one apartment at a time. Please vacate your current apartment first.' +
                    '</div>' +
                    '<div class="d-flex gap-2">' +
                        '<button class="btn btn-secondary" disabled>Book for Myself</button>' +
                        '<button class="btn btn-secondary" disabled>Book in a Group</button>' +
                    '</div>';
            } else {
                html += '<div class="d-flex gap-2">' +
                    '<button class="btn btn-success" id="bookSoloBtn" data-apartment-id="' + apartment.apartmentId + '" data-amount="' + apartment.monthlyRate + '">Book for Myself</button>' +
                    '<button class="btn btn-info" id="bookGroupBtn" data-apartment-id="' + apartment.apartmentId + '" data-amount="' + apartment.monthlyRate + '">Book in a Group</button>' +
                '</div>';
            }
            
            html += '<div id="paymentModalContainer"></div>' +
                '<div id="groupModalContainer"></div>';
            
            document.getElementById('apartmentDetailsModalBody').innerHTML = html;
            var modal = new bootstrap.Modal(document.getElementById('apartmentDetailsModal'));
            modal.show();
        })
        .catch(function() {
            document.getElementById('apartmentDetailsModalBody').innerHTML = '<div class="alert alert-danger">Error loading apartment details.</div>';
            var modal = new bootstrap.Modal(document.getElementById('apartmentDetailsModal'));
            modal.show();
        });
};

// Ensure modals are initialized after DOM is ready
function ensureModalHandlers() {
    // Listen for dynamically created buttons
    document.body.addEventListener('click', function(e) {
        if (e.target && e.target.id === 'bookSoloBtn') {
            var apartmentId = e.target.getAttribute('data-apartment-id');
            var amount = e.target.getAttribute('data-amount');
            console.log('[DEBUG] bookSoloBtn clicked - apartmentId:', apartmentId, 'amount:', amount);
            if (!apartmentId || apartmentId === 'null' || apartmentId === 'undefined') {
                alert('Error: Invalid apartment ID. Please try again.');
                return;
            }
            // Default amount if null/undefined
            if (!amount || amount === 'null' || amount === 'undefined') {
                amount = '1000';
            }
            window.showPaymentModal(apartmentId, amount);
        }
        if (e.target && e.target.id === 'bookGroupBtn') {
            var apartmentId = e.target.getAttribute('data-apartment-id');
            var amount = e.target.getAttribute('data-amount');
            window.showGroupModal(apartmentId, amount);
        }
    });
}

// Call this after DOMContentLoaded
ensureModalHandlers();

// SSLCommerz payment button logic
window.showPaymentModal = function(apartmentId, amount) {
    console.log('[DEBUG] showPaymentModal called with apartmentId:', apartmentId, 'type:', typeof apartmentId);
    
    // CRITICAL: Check if user already has a booking
    if (userHasBooking) {
        alert('You already have an active apartment booking! You can only book one apartment at a time. Please vacate your current apartment first.');
        return;
    }
    
    if (!apartmentId || apartmentId === 'null' || apartmentId === 'undefined') {
        alert('Error: Invalid apartment ID. Cannot proceed with payment.');
        return;
    }
    // Ensure amount is valid, default to 1000 if null/undefined
    if (!amount || amount === 'null' || amount === 'undefined') {
        amount = '1000';
    }
    var user = JSON.parse(localStorage.getItem('user'));
    var html = '<div class="modal fade" id="paymentModal" tabindex="-1" aria-labelledby="paymentModalLabel" aria-hidden="true">' +
        '<div class="modal-dialog">' +
            '<div class="modal-content">' +
                '<div class="modal-header">' +
                    '<h5 class="modal-title" id="paymentModalLabel">Payment</h5>' +
                    '<button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>' +
                '</div>' +
                '<div class="modal-body">' +
                    '<form id="sslcommerzPaymentForm">' +
                        '<div class="mb-2">' +
                            '<label>Amount</label>' +
                            '<input type="number" class="form-control" id="paymentAmount" value="' + amount + '" required />' +
                        '</div>' +
                        '<div class="mb-2">' +
                            '<label>Name</label>' +
                            '<input type="text" class="form-control" id="cusName" value="' + (user.name || '') + '" required />' +
                        '</div>' +
                        '<div class="mb-2">' +
                            '<label>Email</label>' +
                            '<input type="email" class="form-control" id="cusEmail" value="test@example.com" required />' +
                        '</div>' +
                        '<div class="mb-2">' +
                            '<label>Phone</label>' +
                            '<input type="text" class="form-control" id="cusPhone" value="01700000000" required />' +
                        '</div>' +
                        '<button type="submit" class="btn btn-primary">Pay with SSLCommerz</button>' +
                    '</form>' +
                    '<div id="paymentMsg" class="mt-2"></div>' +
                '</div>' +
            '</div>' +
        '</div>' +
    '</div>';
    document.getElementById('paymentModalContainer').innerHTML = html;
    var modal = new bootstrap.Modal(document.getElementById('paymentModal'));
    modal.show();
    setTimeout(function() {
        document.getElementById('sslcommerzPaymentForm').onsubmit = function(e) {
            e.preventDefault();
            var paymentData = {
                    amount: parseFloat(document.getElementById('paymentAmount').value),
                    name: document.getElementById('cusName').value,
                    email: document.getElementById('cusEmail').value,
                    phone: document.getElementById('cusPhone').value,
                    apartmentId: parseInt(apartmentId),
                    tenantId: user ? user.userId : null
            };
            console.log('[DEBUG] Sending payment request:', paymentData);
            console.log('[DEBUG] apartmentId type:', typeof apartmentId, 'value:', apartmentId);
            fetch('/api/payments/initiate', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(paymentData)
            })
            .then(function(res) { 
                if (!res.ok) {
                    // Handle HTTP error codes (409 for conflict, etc.)
                    if (res.status === 409) {
                        // Tenant already has a booking
                        userHasBooking = true;
                        return res.json().then(function(errorData) {
                            throw new Error(errorData.error || 'You already have an active apartment booking.');
                        }).catch(function(parseError) {
                            // If error is already an Error object, re-throw it
                            if (parseError instanceof Error && parseError.message) {
                                throw parseError;
                            }
                            // Otherwise it's a JSON parse error
                            throw new Error('You already have an active apartment booking.');
                        });
                    } else {
                        // Other errors
                        return res.json().then(function(errorData) {
                            throw new Error(errorData.error || 'Payment failed');
                        }).catch(function(parseError) {
                            if (parseError instanceof Error && parseError.message) {
                                throw parseError;
                            }
                            throw new Error('Payment initiation failed');
                        });
                    }
                }
                return res.json(); 
            })
            .then(function(resp) {
                    if (resp && resp.GatewayPageURL) {
                        window.location.href = resp.GatewayPageURL;
                    } else {
                        // Show backend error message if available
                        var errorMsg = resp && typeof resp === 'string' ? resp : (resp && resp.error ? resp.error : JSON.stringify(resp));
                        document.getElementById('paymentMsg').innerHTML = '<span class="text-danger">Failed to initiate payment.<br>' + (errorMsg ? errorMsg : '') + '</span>';
                }
            })
            .catch(function(error) {
                console.error('Payment error:', error);
                var errorMessage = error.message || 'Payment failed';
                
                // Check if this is a booking constraint error
                if (userHasBooking || errorMessage.includes('already have an active apartment booking')) {
                    document.getElementById('paymentMsg').innerHTML = 
                        '<div class="alert alert-danger mt-3">' +
                        '<strong>Booking Constraint:</strong> ' + errorMessage + 
                        '<br><small>You can only book one apartment at a time. Please contact support to change your booking.</small>' +
                        '</div>';
                } else {
                    // Regular error message
                    document.getElementById('paymentMsg').innerHTML = 
                        '<div class="alert alert-danger mt-3">' +
                        '<strong>Error:</strong> ' + errorMessage + 
                        '</div>';
                }
            });
        };
    }, 100);
};

// Group modal logic
window.showGroupModal = function(apartmentId, amount) {
    const user = JSON.parse(localStorage.getItem('user'));
    if (!user) {
        alert('Please log in to book in a group');
        return;
    }
    
    // CRITICAL: Check if user already has a booking
    if (userHasBooking) {
        alert('You already have an active apartment booking! You can only book one apartment at a time. Please vacate your current apartment first.');
        return;
    }

    // Fetch groups for this apartment
    fetch('/api/groups/apartment/' + apartmentId)
        .then(res => res.json())
        .then(data => {
            const formingGroups = data.formingGroups || [];
            const readyGroups = data.readyGroups || [];
            
            // Check if user is in any of these groups
            let userGroup = null;
            [...formingGroups, ...readyGroups].forEach(group => {
                if (group.members && group.members.some(m => m.tenant.userId === user.userId)) {
                    userGroup = group;
                }
            });
            
            let html = '<div class="modal fade" id="groupModal" tabindex="-1" aria-hidden="true">' +
                '<div class="modal-dialog modal-lg">' +
                    '<div class="modal-content">' +
                        '<div class="modal-header">' +
                            '<h5 class="modal-title">Book in a Group (4 People Required)</h5>' +
                            '<button type="button" class="btn-close" data-bs-dismiss="modal"></button>' +
                        '</div>' +
                        '<div class="modal-body">' +
                            '<div id="groupContent">';
            
            // If user is already in a group for this apartment, show their group
            if (userGroup) {
                html += '<div class="alert alert-info">You are already in a group for this apartment!</div>';
                html += '</div><div id="groupMsg" class="mt-3"></div>' +
                        '</div>' +
                    '</div>' +
                '</div>' +
            '</div>';
            
                document.getElementById('groupModalContainer').innerHTML = html;
                const modal = new bootstrap.Modal(document.getElementById('groupModal'));
                modal.show();
                
                // Show the user's group details immediately
                viewMyGroup(userGroup.groupId);
                return;
            }
            
            // Show forming groups
            if (formingGroups.length > 0) {
                html += '<h6>Forming Groups (Join One):</h6>';
                formingGroups.forEach(group => {
                    // Check if user is member
                    const isMember = group.members && group.members.some(m => m.tenant.userId === user.userId);
                    
                    html += '<div class="card mb-2">' +
                        '<div class="card-body">' +
                            '<div class="d-flex justify-content-between align-items-center">' +
                                '<div>' +
                                    '<strong>Group ' + group.groupId + '</strong> - ' + group.memberCount + '/4 members<br>' +
                                    '<small>Invite Code: <code>' + group.inviteCode + '</code></small>' +
                                '</div>' +
                                '<div>';
                    
                    if (isMember) {
                        html += '<button class="btn btn-sm btn-primary me-2" onclick="viewMyGroup(' + group.groupId + ')">View Group</button>' +
                                '<button class="btn btn-sm btn-danger" onclick="leaveGroup(' + group.groupId + ', ' + apartmentId + ')">Leave</button>';
                    } else {
                        html += '<button class="btn btn-sm btn-success" onclick="joinExistingGroup(\'' + group.inviteCode + '\', ' + apartmentId + ')">Join</button>';
                    }
                    
                    html += '</div></div></div></div>';
                });
            }
            
            // Show ready groups
            if (readyGroups.length > 0) {
                html += '<h6 class="mt-3">Ready Groups (4/4 members - Can Book Now):</h6>';
                readyGroups.forEach(group => {
                    const isMember = group.members && group.members.some(m => m.tenant.userId === user.userId);
                    
                    html += '<div class="card mb-2 border-success">' +
                        '<div class="card-body">' +
                            '<div class="d-flex justify-content-between align-items-center">' +
                                '<div>' +
                                    '<strong>Group ' + group.groupId + '</strong> - 4/4 members ✓<br>' +
                                    '<small class="text-success">Ready to book!</small>' +
                                '</div>' +
                                '<div>';
                    
                    if (isMember) {
                        html += '<button class="btn btn-sm btn-primary" onclick="viewMyGroup(' + group.groupId + ')">View & Pay</button>';
                    } else {
                        html += '<span class="badge bg-success">Full</span>';
                    }
                    
                    html += '</div></div></div></div>';
                });
            }
            
            // Create new group button
            html += '<hr><button class="btn btn-primary" onclick="createNewGroup(' + apartmentId + ')">Create New Group</button>';
            
            html += '</div><div id="groupMsg" class="mt-3"></div>' +
                        '</div>' +
                    '</div>' +
                '</div>' +
            '</div>';
            
            document.getElementById('groupModalContainer').innerHTML = html;
            const modal = new bootstrap.Modal(document.getElementById('groupModal'));
            modal.show();
        })
        .catch(error => {
            console.error('Error loading groups:', error);
            alert('Error loading group information');
        });
};

// Create a new group
window.createNewGroup = function(apartmentId) {
    const user = JSON.parse(localStorage.getItem('user'));
    
    fetch('/api/groups/create', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ apartmentId: apartmentId, creatorId: user.userId })
    })
    .then(res => res.json())
    .then(data => {
        if (data.success) {
            const inviteCode = data.inviteCode;
            const inviteLink = window.location.origin + '/join-group?code=' + inviteCode;
            
            document.getElementById('groupContent').innerHTML = 
                '<div class="alert alert-success">' +
                    '<h5>✓ Group Created Successfully!</h5>' +
                    '<p>You are the first member (1/4). Your group status will be shown on the homepage.</p>' +
                    '<div class="card bg-light p-3 mb-3">' +
                        '<h3 class="text-center mb-0"><code>' + inviteCode + '</code></h3>' +
                        '<p class="text-center mb-0 mt-2"><small>Share this invite code with 3 friends</small></p>' +
                    '</div>' +
                    '<p><strong>Share this link:</strong></p>' +
                    '<input type="text" class="form-control mb-2" value="' + inviteLink + '" id="inviteLinkInput" readonly>' +
                    '<button class="btn btn-info" onclick="copyInviteLink()">Copy Link</button>' +
                    '<hr>' +
                    '<p><small>Reloading page to show your group status...</small></p>' +
                '</div>';
            
            // Reload the page after a short delay to show the group status box
            setTimeout(() => {
                location.reload();
            }, 2500);
        } else {
            document.getElementById('groupMsg').innerHTML = '<div class="alert alert-danger">' + data.message + '</div>';
        }
    })
    .catch(error => {
        document.getElementById('groupMsg').innerHTML = '<div class="alert alert-danger">Error: ' + error.message + '</div>';
    });
};

// Join an existing group
window.joinExistingGroup = function(inviteCode, apartmentId) {
    const user = JSON.parse(localStorage.getItem('user'));
    
    if (!user) {
        alert('Please log in first');
        return;
    }
    
    fetch('/api/groups/join', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ inviteCode: inviteCode, tenantId: user.userId })
    })
    .then(res => res.json())
    .then(data => {
        if (data.success) {
            // Show success message
            document.getElementById('groupMsg').innerHTML = 
                '<div class="alert alert-success"><strong>✓ ' + data.message + '</strong><br><small>Reloading page to show your group status...</small></div>';
            
            // Reload the page after a short delay to show the group status box
            setTimeout(() => {
                location.reload();
            }, 1500);
        } else {
            document.getElementById('groupMsg').innerHTML = 
                '<div class="alert alert-danger"><strong>✗ ' + data.message + '</strong></div>';
        }
    })
    .catch(error => {
        document.getElementById('groupMsg').innerHTML = 
            '<div class="alert alert-danger">Error: ' + error.message + '</div>';
    });
};

// View my group
window.viewMyGroup = function(groupId) {
    fetch('/api/groups/' + groupId)
        .then(res => res.json())
        .then(group => {
            const user = JSON.parse(localStorage.getItem('user'));
            const memberCount = group.members ? group.members.length : 0;
            const isFull = memberCount >= 4;
            const isReady = group.status === 'READY';
            
            let html = '<div class="card">' +
                '<div class="card-header bg-primary text-white">' +
                    '<h5>Your Group - ' + memberCount + '/4 Members</h5>' +
                '</div>' +
                '<div class="card-body">' +
                    '<p><strong>Invite Code:</strong> <code>' + group.inviteCode + '</code></p>' +
                    '<p><strong>Status:</strong> <span class="badge bg-' + (isReady ? 'success' : 'warning') + '">' + group.status + '</span></p>' +
                    '<h6>Members:</h6><ul class="list-group mb-3">';
            
            if (group.members) {
                group.members.forEach(member => {
                    html += '<li class="list-group-item">' + member.tenant.name + 
                        (member.tenant.userId === group.creatorId ? ' <span class="badge bg-primary">Creator</span>' : '') +
                        '</li>';
                });
            }
            
            html += '</ul>';
            
            // Show pay button if group is full and ready
            if (isFull && isReady) {
                html += '<div class="alert alert-success"><strong>✓ Group is Complete!</strong><br>Any member can now pay to book the apartment.</div>' +
                    '<button class="btn btn-success btn-lg w-100 mb-3" onclick="bookApartmentForGroup(' + groupId + ', ' + group.apartment.apartmentId + ', ' + group.apartment.monthlyRate + ')">💳 Pay Now & Book Apartment ($' + group.apartment.monthlyRate + ')</button>';
            } else {
                html += '<div class="alert alert-info"><strong>⏳ Waiting for more members...</strong><br>Need ' + (4 - memberCount) + ' more member(s) to join before booking.</div>';
            }
            
            html += '<hr><div class="d-flex gap-2">' +
                '<button class="btn btn-secondary flex-fill" onclick="showGroupModal(' + group.apartment.apartmentId + ')">« Back to Groups</button>' +
                '<button class="btn btn-danger flex-fill" onclick="leaveGroup(' + groupId + ', ' + group.apartment.apartmentId + ')">Leave Group</button>' +
                '</div>';
            html += '</div></div>';
            
            document.getElementById('groupContent').innerHTML = html;
            document.getElementById('groupMsg').innerHTML = '';
        })
        .catch(error => {
            document.getElementById('groupMsg').innerHTML = 
                '<div class="alert alert-danger">Error loading group: ' + error.message + '</div>';
        });
};

// Leave group
window.leaveGroup = function(groupId, apartmentId) {
    if (!confirm('Are you sure you want to leave this group?')) return;
    
    const user = JSON.parse(localStorage.getItem('user'));
    
    fetch('/api/groups/' + groupId + '/leave', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ tenantId: user.userId })
    })
    .then(res => res.json())
    .then(data => {
        if (data.success) {
            document.getElementById('groupMsg').innerHTML = 
                '<div class="alert alert-success">' + data.message + '</div>';
            // Refresh the group list to show updated member count
            setTimeout(() => {
                if (apartmentId) {
                    showGroupModal(apartmentId);
                } else {
                    // If no apartmentId provided, just close modal
                    bootstrap.Modal.getInstance(document.getElementById('groupModal')).hide();
                }
            }, 1000);
        } else {
            document.getElementById('groupMsg').innerHTML = 
                '<div class="alert alert-danger">' + data.message + '</div>';
        }
    })
    .catch(error => {
        document.getElementById('groupMsg').innerHTML = 
            '<div class="alert alert-danger">Error: ' + error.message + '</div>';
    });
};

// Book apartment for group (payment with SSLCommerz)
window.bookApartmentForGroup = function(groupId, apartmentId, amount) {
    const user = JSON.parse(localStorage.getItem('user'));
    
    if (!user) {
        alert('Please log in first');
        return;
    }
    
    // Show payment modal with SSLCommerz integration
    var html = '<div class="modal fade" id="groupPaymentModal" tabindex="-1" aria-hidden="true">' +
        '<div class="modal-dialog">' +
            '<div class="modal-content">' +
                '<div class="modal-header bg-success text-white">' +
                    '<h5 class="modal-title">💳 Group Payment</h5>' +
                    '<button type="button" class="btn-close" data-bs-dismiss="modal"></button>' +
                '</div>' +
                '<div class="modal-body">' +
                    '<div class="alert alert-info">' +
                        '<strong>Group Booking:</strong> You are paying on behalf of your group.<br>' +
                        '<small>Group ID: ' + groupId + '</small>' +
                    '</div>' +
                    '<form id="groupSslcommerzPaymentForm">' +
                        '<div class="mb-3">' +
                            '<label class="form-label">Amount (BDT)</label>' +
                            '<input type="number" class="form-control" id="groupPaymentAmount" value="' + amount + '" required readonly />' +
                        '</div>' +
                        '<div class="mb-3">' +
                            '<label class="form-label">Name</label>' +
                            '<input type="text" class="form-control" id="groupCusName" value="' + (user.name || '') + '" required />' +
                        '</div>' +
                        '<div class="mb-3">' +
                            '<label class="form-label">Email</label>' +
                            '<input type="email" class="form-control" id="groupCusEmail" value="' + (user.email || 'test@example.com') + '" required />' +
                        '</div>' +
                        '<div class="mb-3">' +
                            '<label class="form-label">Phone</label>' +
                            '<input type="text" class="form-control" id="groupCusPhone" value="01700000000" required />' +
                        '</div>' +
                        '<button type="submit" class="btn btn-success w-100 btn-lg">' +
                            '<i class="bi bi-credit-card"></i> Pay ৳' + amount + ' with SSLCommerz' +
                        '</button>' +
                    '</form>' +
                    '<div id="groupPaymentMsg" class="mt-3"></div>' +
                '</div>' +
            '</div>' +
        '</div>' +
    '</div>';
    
    // Insert modal into groupModalContainer or create a dedicated container
    const container = document.getElementById('groupModalContainer') || document.getElementById('paymentModalContainer');
    if (container) {
        container.innerHTML = html;
    } else {
        // Create container if it doesn't exist
        const newContainer = document.createElement('div');
        newContainer.id = 'groupPaymentModalContainer';
        document.body.appendChild(newContainer);
        newContainer.innerHTML = html;
    }
    
    const modal = new bootstrap.Modal(document.getElementById('groupPaymentModal'));
    modal.show();
    
    // Handle form submission after modal is shown
    setTimeout(function() {
        document.getElementById('groupSslcommerzPaymentForm').onsubmit = function(e) {
            e.preventDefault();
            
            const paymentData = {
                amount: parseFloat(document.getElementById('groupPaymentAmount').value),
                name: document.getElementById('groupCusName').value,
                email: document.getElementById('groupCusEmail').value,
                phone: document.getElementById('groupCusPhone').value,
                apartmentId: parseInt(apartmentId),
                tenantId: user.userId,
                groupId: groupId // Include groupId for backend processing
            };
            
            console.log('[DEBUG] Initiating group payment:', paymentData);
            
            // First, verify the group is ready and user is a member
            fetch('/api/groups/' + groupId)
                .then(res => res.json())
                .then(group => {
                    if (group.status !== 'READY') {
                        throw new Error('Group is not ready for booking. All 4 members must join first.');
                    }
                    if (!group.members.some(m => m.tenant.userId === user.userId)) {
                        throw new Error('You are not a member of this group.');
                    }
                    
                    // Proceed with payment initiation
                    return fetch('/api/payments/initiate-group', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify(paymentData)
                    });
                })
                .then(function(res) { 
                    if (!res.ok) {
                        if (res.status === 409) {
                            return res.json().then(function(errorData) {
                                throw new Error(errorData.error || 'Payment conflict occurred.');
                            });
                        } else {
                            return res.json().then(function(errorData) {
                                throw new Error(errorData.error || 'Payment failed');
                            });
                        }
                    }
                    return res.json(); 
                })
                .then(function(resp) {
                    if (resp && resp.GatewayPageURL) {
                        // Store group context for payment callback
                        localStorage.setItem('pendingGroupBooking', JSON.stringify({
                            groupId: groupId,
                            apartmentId: apartmentId,
                            timestamp: Date.now()
                        }));
                        // Redirect to SSLCommerz payment gateway
                        window.location.href = resp.GatewayPageURL;
                    } else {
                        var errorMsg = resp && typeof resp === 'string' ? resp : (resp && resp.error ? resp.error : JSON.stringify(resp));
                        document.getElementById('groupPaymentMsg').innerHTML = 
                            '<div class="alert alert-danger">Failed to initiate payment.<br>' + (errorMsg ? errorMsg : '') + '</div>';
                    }
                })
                .catch(function(error) {
                    console.error('Group payment error:', error);
                    document.getElementById('groupPaymentMsg').innerHTML = 
                        '<div class="alert alert-danger"><strong>Error:</strong> ' + error.message + '</div>';
                });
        };
    }, 100);
};

// Copy invite link
window.copyInviteLink = function() {
    const input = document.getElementById('inviteLinkInput');
    input.select();
    document.execCommand('copy');
    alert('Invite link copied to clipboard!');
};

// ============ REVIEW SYSTEM ============

// Show review modal after vacating
function showReviewModal(apartmentId) {
    const user = JSON.parse(localStorage.getItem('user'));
    if (!user) {
        alert('Please login to leave a review');
        return;
    }
    
    const html = `
        <div class="modal fade" id="reviewModal" tabindex="-1">
            <div class="modal-dialog">
                <div class="modal-content">
                    <div class="modal-header">
                        <h5 class="modal-title"><i class="bi bi-star-fill text-warning"></i> Leave a Review</h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                    </div>
                    <div class="modal-body">
                        <form id="reviewForm">
                            <div class="mb-3">
                                <label class="form-label">Rating *</label>
                                <div class="star-rating" id="starRating">
                                    <i class="bi bi-star rating-star" data-rating="1"></i>
                                    <i class="bi bi-star rating-star" data-rating="2"></i>
                                    <i class="bi bi-star rating-star" data-rating="3"></i>
                                    <i class="bi bi-star rating-star" data-rating="4"></i>
                                    <i class="bi bi-star rating-star" data-rating="5"></i>
                                </div>
                                <input type="hidden" id="ratingValue" required>
                            </div>
                            
                            <div class="mb-3">
                                <label for="goodSides" class="form-label">
                                    <i class="bi bi-hand-thumbs-up text-success"></i> What did you like? *
                                </label>
                                <textarea class="form-control" id="goodSides" rows="3" 
                                    maxlength="500" placeholder="Share the positive aspects of your stay..." required></textarea>
                                <small class="text-muted">Max 500 characters</small>
                            </div>
                            
                            <div class="mb-3">
                                <label for="badSides" class="form-label">
                                    <i class="bi bi-hand-thumbs-down text-danger"></i> What could be improved?
                                </label>
                                <textarea class="form-control" id="badSides" rows="3" 
                                    maxlength="500" placeholder="Share areas that need improvement (optional)"></textarea>
                                <small class="text-muted">Max 500 characters</small>
                            </div>
                            
                            <div id="reviewMessage"></div>
                            
                            <div class="d-flex gap-2">
                                <button type="submit" class="btn btn-primary">Submit Review</button>
                                <button type="button" class="btn btn-secondary" onclick="window.location.reload()">Skip</button>
                            </div>
                        </form>
                    </div>
                </div>
            </div>
        </div>
    `;
    
    // Remove existing modal if any
    const existingModal = document.getElementById('reviewModal');
    if (existingModal) existingModal.remove();
    
    document.body.insertAdjacentHTML('beforeend', html);
    const modal = new bootstrap.Modal(document.getElementById('reviewModal'));
    modal.show();
    
    // Star rating functionality
    let selectedRating = 0;
    const stars = document.querySelectorAll('.rating-star');
    stars.forEach(star => {
        star.addEventListener('click', function() {
            selectedRating = parseInt(this.dataset.rating);
            document.getElementById('ratingValue').value = selectedRating;
            updateStarDisplay(selectedRating);
        });
        
        star.addEventListener('mouseenter', function() {
            const rating = parseInt(this.dataset.rating);
            updateStarDisplay(rating);
        });
    });
    
    document.getElementById('starRating').addEventListener('mouseleave', function() {
        updateStarDisplay(selectedRating);
    });
    
    function updateStarDisplay(rating) {
        stars.forEach((star, index) => {
            if (index < rating) {
                star.classList.remove('bi-star');
                star.classList.add('bi-star-fill', 'text-warning');
            } else {
                star.classList.remove('bi-star-fill', 'text-warning');
                star.classList.add('bi-star');
            }
        });
    }
    
    // Handle form submission
    document.getElementById('reviewForm').addEventListener('submit', function(e) {
        e.preventDefault();
        
        const rating = document.getElementById('ratingValue').value;
        if (!rating) {
            document.getElementById('reviewMessage').innerHTML = 
                '<div class="alert alert-warning">Please select a rating</div>';
            return;
        }
        
        const goodSides = document.getElementById('goodSides').value.trim();
        const badSides = document.getElementById('badSides').value.trim();
        
        fetch('/api/reviews/submit', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                userId: user.userId,
                apartmentId: apartmentId,
                rating: parseInt(rating),
                goodSides: goodSides,
                badSides: badSides || 'No concerns mentioned'
            })
        })
        .then(response => response.json())
        .then(data => {
            if (data.message) {
                document.getElementById('reviewMessage').innerHTML = 
                    `<div class="alert alert-success">
                        <strong>Thank you!</strong> ${data.message}
                    </div>`;
                setTimeout(() => {
                    window.location.reload();
                }, 2000);
            } else {
                document.getElementById('reviewMessage').innerHTML = 
                    `<div class="alert alert-danger">${data.error}</div>`;
            }
        })
        .catch(error => {
            console.error('Error:', error);
            document.getElementById('reviewMessage').innerHTML = 
                `<div class="alert alert-danger">Failed to submit review</div>`;
        });
    });
}

// Load and display reviews for an apartment
function loadApartmentReviews(apartmentId) {
    fetch(`/api/reviews/apartment/${apartmentId}`)
        .then(response => response.json())
        .then(data => {
            displayReviews(data.reviews, data.averageRating, data.totalReviews);
        })
        .catch(error => {
            console.error('Error loading reviews:', error);
        });
}

function displayReviews(reviews, avgRating, totalReviews) {
    const container = document.getElementById('reviewsContainer');
    if (!container) return;
    
    if (totalReviews === 0) {
        container.innerHTML = `
            <div class="text-center text-muted py-4">
                <i class="bi bi-chat-quote" style="font-size: 3rem;"></i>
                <p>No reviews yet. Be the first to share your experience!</p>
            </div>`;
        return;
    }
    
    let html = `
        <div class="reviews-summary mb-4">
            <div class="row align-items-center">
                <div class="col-md-4 text-center">
                    <h2 class="mb-0">${avgRating.toFixed(1)}</h2>
                    <div class="star-display mb-2">
                        ${getStarHTML(Math.round(avgRating))}
                    </div>
                    <p class="text-muted">${totalReviews} review${totalReviews > 1 ? 's' : ''}</p>
                </div>
                <div class="col-md-8">
                    <p class="mb-0">Based on ${totalReviews} tenant review${totalReviews > 1 ? 's' : ''}</p>
                </div>
            </div>
        </div>
        <hr>
    `;
    
    reviews.forEach(review => {
        const reviewDate = new Date(review.date).toLocaleDateString('en-US', {
            year: 'numeric', month: 'short', day: 'numeric'
        });
        
        html += `
            <div class="review-card mb-3">
                <div class="d-flex justify-content-between align-items-start mb-2">
                    <div>
                        <h6 class="mb-1">${review.tenantName || 'Anonymous'}</h6>
                        <div class="star-display">
                            ${getStarHTML(review.rating)}
                        </div>
                    </div>
                    <small class="text-muted">${reviewDate}</small>
                </div>
                
                ${review.goodSides ? `
                    <div class="mb-2">
                        <strong class="text-success">
                            <i class="bi bi-hand-thumbs-up"></i> Liked:
                        </strong>
                        <p class="mb-0 ms-3">${review.goodSides}</p>
                    </div>
                ` : ''}
                
                ${review.badSides && review.badSides !== 'No concerns mentioned' ? `
                    <div class="mb-2">
                        <strong class="text-danger">
                            <i class="bi bi-hand-thumbs-down"></i> Could improve:
                        </strong>
                        <p class="mb-0 ms-3">${review.badSides}</p>
                    </div>
                ` : ''}
            </div>
            <hr>
        `;
    });
    
    container.innerHTML = html;
}

function getStarHTML(rating) {
    let html = '';
    for (let i = 1; i <= 5; i++) {
        if (i <= rating) {
            html += '<i class="bi bi-star-fill text-warning"></i>';
        } else {
            html += '<i class="bi bi-star text-warning"></i>';
        }
    }
    return html;
}