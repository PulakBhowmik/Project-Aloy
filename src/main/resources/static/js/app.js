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
                // DO NOT show banner - let the payment modal handle the error
            } else {
                userHasBooking = false;
                userBookingDetails = null;
                console.log('[INFO] User has no existing booking');
            }
        })
        .catch(error => {
            console.error('[ERROR] Failed to check tenant booking status:', error);
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
        // Check if apartment is booked
        const isBooked = apartment.booked || apartment.status === 'RENTED';
        const statusBadge = isBooked ? '<span class="badge bg-danger">BOOKED</span>' : '<span class="badge bg-success">AVAILABLE</span>';
        
        html += `
        <div class="col-md-6 col-lg-4 mb-4">
            <div class="card apartment-card h-100">
                <img src="https://placehold.co/600x400/4f46e5/ffffff?text=${encodeURIComponent(apartment.title)}" class="card-img-top" alt="${apartment.title}">
                <div class="card-body d-flex flex-column">
                    <div class="d-flex justify-content-between align-items-center mb-2">
                        <div class="price-tag">$${apartment.monthlyRate}/month</div>
                        ${statusBadge}
                    </div>
                    <h5 class="card-title">${apartment.title}</h5>
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
            // Don't disable buttons - let the payment modal show the error
            var html = '<img src="https://placehold.co/800x400/4f46e5/ffffff?text=' + encodeURIComponent(apartment.title) + '" class="img-fluid mb-3" alt="' + apartment.title + '">' +
                '<h3>' + apartment.title + '</h3>' +
                '<p><strong>Description:</strong> ' + (apartment.description || 'No description available') + '</p>' +
                '<ul class="list-group mb-3">' +
                    '<li class="list-group-item"><strong>Location:</strong> ' + (apartment.address || 'N/A') + '</li>' +
                    '<li class="list-group-item"><strong>District:</strong> ' + (apartment.district || 'N/A') + '</li>' +
                    '<li class="list-group-item"><strong>Available from:</strong> ' + formatDate(apartment.availability) + '</li>' +
                    '<li class="list-group-item"><strong>Suitable for:</strong> ' + (apartment.allowedFor === 'solo' ? 'Solo' : (apartment.allowedFor === 'group' ? 'Group' : (apartment.allowedFor === 'both' ? 'Both' : 'Everyone'))) + '</li>' +
                    '<li class="list-group-item"><strong>Monthly Rate:</strong> $' + apartment.monthlyRate + '</li>' +
                '</ul>' +
                '<p><strong>Owner ID:</strong> ' + (apartment.ownerId || 'N/A') + '</p>' +
                '<div class="d-flex gap-2">' +
                    '<button class="btn btn-success" id="bookSoloBtn" data-apartment-id="' + apartment.apartmentId + '" data-amount="' + apartment.monthlyRate + '">Book for Myself</button>' +
                    '<button class="btn btn-info" id="bookGroupBtn" data-apartment-id="' + apartment.apartmentId + '" data-amount="' + apartment.monthlyRate + '">Book in a Group</button>' +
                '</div>' +
                '<div id="paymentModalContainer"></div>' +
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
    var user = JSON.parse(localStorage.getItem('user'));
    fetch('/api/roommate-groups/by-apartment/' + apartmentId)
        .then(function(res) { return res.json(); })
        .then(function(groups) {
            var group = groups.find(function(g) {
                return g.members.some(function(m) { return m.tenant.userId === user.userId; });
            });
            var html = '<div class="modal fade" id="groupModal" tabindex="-1" aria-labelledby="groupModalLabel" aria-hidden="true">' +
                '<div class="modal-dialog">' +
                    '<div class="modal-content">' +
                        '<div class="modal-header">' +
                            '<h5 class="modal-title" id="groupModalLabel">Roommate Group</h5>' +
                            '<button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>' +
                        '</div>' +
                        '<div class="modal-body">' +
                            '<div id="groupMembersList">';
            if (group) {
                html += '<h6>Group Members:</h6><ul>';
                group.members.forEach(function(m) {
                    html += '<li>' + m.tenant.name + ' (' + m.tenant.userId + ')</li>';
                });
                html += '</ul>';
                if (group.members.length < 4) {
                    html += '<p>Waiting for more members to join...</p>';
                } else {
                    html += '<button class="btn btn-primary" id="groupPayBtn">Book/Make Payment</button>';
                }
                html += '<button class="btn btn-danger mt-2" id="leaveGroupBtn">Leave Group</button>';
            } else {
                html += '<p>You are not in a group for this apartment. <button class="btn btn-success" id="joinGroupBtn">Join Group</button></p>';
            }
            html += '</div><div id="groupMsg" class="mt-2"></div></div></div></div></div>';
            document.getElementById('groupModalContainer').innerHTML = html;
            var modal = new bootstrap.Modal(document.getElementById('groupModal'));
            modal.show();
            setTimeout(function() {
                if (document.getElementById('groupPayBtn')) {
                    document.getElementById('groupPayBtn').onclick = function() {
                        window.showPaymentModal(apartmentId, amount);
                    };
                }
                if (document.getElementById('leaveGroupBtn')) {
                    document.getElementById('leaveGroupBtn').onclick = function() {
                        document.getElementById('groupMsg').innerHTML = '<span class="text-info">Leave group feature coming soon.</span>';
                    };
                }
                if (document.getElementById('joinGroupBtn')) {
                    document.getElementById('joinGroupBtn').onclick = function() {
                        document.getElementById('groupMsg').innerHTML = '<span class="text-info">Join group feature coming soon.</span>';
                    };
                }
            }, 100);
        });
};