// Owner Dashboard JavaScript

let currentUser = null;
let ownerApartments = [];

// Initialize on page load
document.addEventListener('DOMContentLoaded', function() {
    // Check if user is logged in and is an owner
    currentUser = JSON.parse(localStorage.getItem('user'));
    
    if (!currentUser || !currentUser.userId) {
        alert('Please login first');
        window.location.href = '/login';
        return;
    }
    
    if (currentUser.role !== 'OWNER') {
        alert('This page is only accessible to owners');
        window.location.href = '/';
        return;
    }
    
    renderNavbarUserInfo();
    loadOwnerApartments();
});

// Render navbar user info
function renderNavbarUserInfo() {
    const navbar = document.getElementById('navbarUserInfo');
    if (currentUser) {
        navbar.innerHTML = `
            <span class="nav-link">ID: ${currentUser.userId} | ${currentUser.name} (${currentUser.role})</span>
            <a class="nav-link" href="#" onclick="logoutUser()">Logout</a>
        `;
    }
}

// Logout function
function logoutUser() {
    localStorage.removeItem('user');
    window.location.href = '/login';
}

// Load owner's apartments
function loadOwnerApartments() {
    fetch(`/api/apartments/owner/${currentUser.userId}`)
        .then(response => response.json())
        .then(apartments => {
            ownerApartments = apartments;
            renderApartments(apartments);
            loadAnalytics(); // Load comprehensive analytics
        })
        .catch(error => {
            console.error('Error loading apartments:', error);
            document.getElementById('ownerApartmentsContainer').innerHTML = 
                '<div class="col-12"><div class="alert alert-danger">Failed to load apartments</div></div>';
        });
}

// Load comprehensive analytics from backend
function loadAnalytics() {
    fetch(`/api/owner-analytics/owner/${currentUser.userId}`)
        .then(response => response.json())
        .then(data => {
            // Update stats from analytics endpoint
            document.getElementById('totalApartments').textContent = data.totalApartments || 0;
            document.getElementById('availableApartments').textContent = data.availableApartments || 0;
            document.getElementById('rentedApartments').textContent = data.rentedApartments || 0;
            document.getElementById('totalRevenue').textContent = `৳${(data.monthlyRevenue || 0).toFixed(2)}`;
            document.getElementById('averageRent').textContent = `৳${(data.averageRent || 0).toFixed(2)}`;
            document.getElementById('occupancyRate').textContent = `${data.occupancyRate || 0}%`;
        })
        .catch(error => console.error('Error loading analytics:', error));
}

// Load tenants information
function loadTenants() {
    fetch(`/api/owner-analytics/owner/${currentUser.userId}/tenants`)
        .then(response => response.json())
        .then(tenants => {
            const container = document.getElementById('tenantsContainer');
            
            if (tenants.length === 0) {
                container.innerHTML = '<p class="text-muted">No tenants currently renting your apartments.</p>';
                return;
            }
            
            let html = '<div class="table-responsive"><table class="table table-hover">';
            html += '<thead class="table-light"><tr>';
            html += '<th>Apartment</th><th>Tenant Name</th><th>Email</th><th>Rent</th><th>Payment Date</th><th>Vacate Date</th>';
            html += '</tr></thead><tbody>';
            
            tenants.forEach(tenant => {
                html += '<tr>';
                html += `<td><strong>${tenant.apartmentTitle}</strong><br><small class="text-muted">${tenant.district}</small></td>`;
                html += `<td>${tenant.tenantName || 'N/A'}</td>`;
                html += `<td>${tenant.tenantEmail || 'N/A'}</td>`;
                html += `<td><strong>৳${tenant.monthlyRent}</strong></td>`;
                html += `<td>${formatDate(tenant.paymentDate)}</td>`;
                html += `<td>${tenant.vacateDate ? formatDate(tenant.vacateDate) : '<span class="badge bg-success">Active</span>'}</td>`;
                html += '</tr>';
            });
            
            html += '</tbody></table></div>';
            container.innerHTML = html;
        })
        .catch(error => {
            console.error('Error loading tenants:', error);
            document.getElementById('tenantsContainer').innerHTML = '<p class="text-danger">Failed to load tenant information</p>';
        });
}

// Load revenue by district
function loadRevenueByDistrict() {
    fetch(`/api/owner-analytics/owner/${currentUser.userId}/revenue-by-district`)
        .then(response => response.json())
        .then(data => {
            const container = document.getElementById('revenueContainer');
            
            if (data.length === 0) {
                container.innerHTML = '<p class="text-muted">No revenue data available yet.</p>';
                return;
            }
            
            let html = '<div class="table-responsive"><table class="table">';
            html += '<thead class="table-light"><tr>';
            html += '<th>District</th><th>Rented Apartments</th><th>Monthly Revenue</th>';
            html += '</tr></thead><tbody>';
            
            data.forEach(district => {
                html += '<tr>';
                html += `<td><strong>${district.district}</strong></td>`;
                html += `<td><span class="badge bg-primary">${district.count}</span></td>`;
                html += `<td><strong class="text-success">৳${district.revenue.toFixed(2)}</strong></td>`;
                html += '</tr>';
            });
            
            html += '</tbody></table></div>';
            
            // Add visual bar chart
            html += '<div class="mt-4"><h6>Visual Representation:</h6>';
            const maxRevenue = Math.max(...data.map(d => d.revenue));
            data.forEach(district => {
                const percentage = maxRevenue > 0 ? (district.revenue / maxRevenue * 100) : 0;
                html += `
                    <div class="mb-2">
                        <div class="d-flex justify-content-between">
                            <span>${district.district}</span>
                            <span class="text-success fw-bold">$${district.revenue.toFixed(2)}</span>
                        </div>
                        <div class="progress" style="height: 25px;">
                            <div class="progress-bar bg-success" style="width: ${percentage}%">${district.count} apt(s)</div>
                        </div>
                    </div>
                `;
            });
            html += '</div>';
            
            container.innerHTML = html;
        })
        .catch(error => {
            console.error('Error loading revenue:', error);
            document.getElementById('revenueContainer').innerHTML = '<p class="text-danger">Failed to load revenue data</p>';
        });
}

// Load payment history
function loadPaymentHistory() {
    fetch(`/api/owner-analytics/owner/${currentUser.userId}/payment-history?limit=20`)
        .then(response => response.json())
        .then(payments => {
            const container = document.getElementById('historyContainer');
            
            if (payments.length === 0) {
                container.innerHTML = '<p class="text-muted">No payment history available.</p>';
                return;
            }
            
            let html = '<div class="table-responsive"><table class="table table-striped">';
            html += '<thead class="table-dark"><tr>';
            html += '<th>Date</th><th>Apartment</th><th>Tenant</th><th>Transaction ID</th><th>Amount</th><th>Status</th>';
            html += '</tr></thead><tbody>';
            
            payments.forEach(payment => {
                const statusClass = payment.status === 'COMPLETED' ? 'success' : 
                                   payment.status === 'VACATED' ? 'warning' : 'secondary';
                html += '<tr>';
                html += `<td>${formatDate(payment.paymentDate)}</td>`;
                html += `<td>${payment.apartmentTitle}</td>`;
                html += `<td>${payment.tenantName || 'N/A'}</td>`;
                html += `<td><code>${payment.transactionId}</code></td>`;
                html += `<td><strong>৳${payment.amount}</strong></td>`;
                html += `<td><span class="badge bg-${statusClass}">${payment.status}</span></td>`;
                html += '</tr>';
            });
            
            html += '</tbody></table></div>';
            container.innerHTML = html;
        })
        .catch(error => {
            console.error('Error loading payment history:', error);
            document.getElementById('historyContainer').innerHTML = '<p class="text-danger">Failed to load payment history</p>';
        });
}

// Load top performing apartments
function loadTopApartments() {
    fetch(`/api/owner-analytics/owner/${currentUser.userId}/top-apartments?limit=10`)
        .then(response => response.json())
        .then(apartments => {
            const container = document.getElementById('topApartmentsContainer');
            
            if (apartments.length === 0) {
                container.innerHTML = '<p class="text-muted">No apartment performance data available.</p>';
                return;
            }
            
            let html = '<div class="table-responsive"><table class="table table-hover">';
            html += '<thead class="table-light"><tr>';
            html += '<th>Rank</th><th>Apartment</th><th>Location</th><th>Bookings</th><th>Total Revenue</th><th>Status</th>';
            html += '</tr></thead><tbody>';
            
            apartments.forEach((apt, index) => {
                const rankBadge = index === 0 ? '🥇' : index === 1 ? '🥈' : index === 2 ? '🥉' : `${index + 1}`;
                const statusBadge = apt.status === 'AVAILABLE' ? 
                    '<span class="badge bg-success">Available</span>' : 
                    '<span class="badge bg-warning">Booked</span>';
                    
                html += '<tr>';
                html += `<td><h4>${rankBadge}</h4></td>`;
                html += `<td><strong>${apt.title}</strong><br><small>৳${apt.monthlyRent}/mo</small></td>`;
                html += `<td>${apt.district}</td>`;
                html += `<td><span class="badge bg-info">${apt.bookingCount}</span></td>`;
                html += `<td><strong class="text-success">৳${apt.totalRevenue.toFixed(2)}</strong></td>`;
                html += `<td>${statusBadge}</td>`;
                html += '</tr>';
            });
            
            html += '</tbody></table></div>';
            container.innerHTML = html;
        })
        .catch(error => {
            console.error('Error loading top apartments:', error);
            document.getElementById('topApartmentsContainer').innerHTML = '<p class="text-danger">Failed to load top apartments</p>';
        });
}

// Format date helper
function formatDate(dateString) {
    if (!dateString) return 'N/A';
    try {
        const date = new Date(dateString);
        return date.toLocaleDateString('en-US', { year: 'numeric', month: 'short', day: 'numeric' });
    } catch (e) {
        return dateString;
    }
}

// Tab change event listeners
document.addEventListener('DOMContentLoaded', function() {
    const tabs = document.querySelectorAll('button[data-bs-toggle="tab"]');
    tabs.forEach(tab => {
        tab.addEventListener('shown.bs.tab', function(event) {
            const targetId = event.target.getAttribute('data-bs-target');
            
            if (targetId === '#tenants') {
                loadTenants();
            } else if (targetId === '#revenue') {
                loadRevenueByDistrict();
            } else if (targetId === '#history') {
                loadPaymentHistory();
            } else if (targetId === '#top') {
                loadTopApartments();
            }
        });
    });
});

// Render apartments
function renderApartments(apartments) {
    const container = document.getElementById('ownerApartmentsContainer');
    
    if (apartments.length === 0) {
        container.innerHTML = `
            <div class="col-12">
                <div class="alert alert-info">
                    <h4><i class="bi bi-info-circle"></i> No Apartments Yet</h4>
                    <p>You haven't added any apartments yet. Go to the <a href="/">home page</a> to add your first apartment!</p>
                </div>
            </div>
        `;
        return;
    }
    
    container.innerHTML = apartments.map(apartment => {
        const statusBadge = apartment.status === 'AVAILABLE' 
            ? '<span class="badge bg-success">Available</span>'
            : apartment.status === 'BOOKED'
            ? '<span class="badge bg-warning text-dark">Booked</span>'
            : '<span class="badge bg-secondary">Unknown</span>';
            
        const bookedBadge = apartment.booked 
            ? '<span class="badge bg-danger ms-2">Booked</span>'
            : '<span class="badge bg-success ms-2">Not Booked</span>';
            
        return `
            <div class="col-md-6 col-lg-4 mb-4">
                <div class="card h-100">
                    <div class="card-body">
                        <h5 class="card-title">${apartment.title || 'Untitled Apartment'}</h5>
                        <p class="card-text">
                            <i class="bi bi-geo-alt"></i> ${apartment.district || 'N/A'}<br>
                            <i class="bi bi-house"></i> ${apartment.address || apartment.street || 'Address not set'}<br>
                            <i class="bi bi-cash-stack"></i> <strong>$${apartment.monthlyRate || 0}/month</strong><br>
                            <i class="bi bi-calendar"></i> Available: ${apartment.availability || 'Immediate'}<br>
                            <i class="bi bi-people"></i> ${apartment.allowedFor || 'N/A'}
                        </p>
                        <div class="mb-2">
                            ${statusBadge}
                            ${bookedBadge}
                        </div>
                        <div class="d-grid gap-2">
                            <button class="btn btn-primary btn-sm" onclick="openEditModal(${apartment.apartmentId})">
                                <i class="bi bi-pencil"></i> Edit
                            </button>
                            <button class="btn btn-danger btn-sm" onclick="openDeleteModal(${apartment.apartmentId})">
                                <i class="bi bi-trash"></i> Delete
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        `;
    }).join('');
}

// Open edit modal
window.openEditModal = function(apartmentId) {
    const apartment = ownerApartments.find(a => a.apartmentId === apartmentId);
    if (!apartment) {
        alert('Apartment not found');
        return;
    }
    
    // Fill form with current values
    document.getElementById('editApartmentId').value = apartment.apartmentId;
    document.getElementById('editTitle').value = apartment.title || '';
    document.getElementById('editHouseNo').value = apartment.houseNo || '';
    document.getElementById('editStreet').value = apartment.street || '';
    document.getElementById('editDistrict').value = apartment.district || '';
    document.getElementById('editAddress').value = apartment.address || '';
    document.getElementById('editDescription').value = apartment.description || '';
    document.getElementById('editMonthlyRate').value = apartment.monthlyRate || '';
    document.getElementById('editAvailability').value = apartment.availability || '';
    document.getElementById('editAllowedFor').value = apartment.allowedFor || 'both';
    
    // Clear previous messages
    document.getElementById('editMessage').innerHTML = '';
    
    // Show modal
    const modal = new bootstrap.Modal(document.getElementById('editApartmentModal'));
    modal.show();
};

// Handle edit form submission
document.getElementById('editApartmentForm').addEventListener('submit', function(e) {
    e.preventDefault();
    
    const apartmentId = document.getElementById('editApartmentId').value;
    const updatedApartment = {
        ownerId: currentUser.userId, // For verification
        title: document.getElementById('editTitle').value,
        houseNo: document.getElementById('editHouseNo').value,
        street: document.getElementById('editStreet').value,
        district: document.getElementById('editDistrict').value,
        address: document.getElementById('editAddress').value,
        description: document.getElementById('editDescription').value,
        monthlyRate: parseFloat(document.getElementById('editMonthlyRate').value),
        availability: document.getElementById('editAvailability').value || null,
        allowedFor: document.getElementById('editAllowedFor').value
    };
    
    fetch(`/api/apartments/${apartmentId}`, {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(updatedApartment)
    })
    .then(response => {
        if (response.ok) {
            return response.json();
        } else if (response.status === 403) {
            throw new Error('You can only edit your own apartments');
        } else {
            throw new Error('Failed to update apartment');
        }
    })
    .then(data => {
        document.getElementById('editMessage').innerHTML = 
            '<div class="alert alert-success">Apartment updated successfully!</div>';
        
        // Reload apartments after 1 second
        setTimeout(() => {
            bootstrap.Modal.getInstance(document.getElementById('editApartmentModal')).hide();
            loadOwnerApartments();
        }, 1000);
    })
    .catch(error => {
        console.error('Error:', error);
        document.getElementById('editMessage').innerHTML = 
            `<div class="alert alert-danger">Error: ${error.message}</div>`;
    });
});

// Open delete modal
window.openDeleteModal = function(apartmentId) {
    document.getElementById('deleteApartmentId').value = apartmentId;
    const modal = new bootstrap.Modal(document.getElementById('deleteConfirmModal'));
    modal.show();
};

// Confirm delete apartment
window.confirmDeleteApartment = function() {
    const apartmentId = document.getElementById('deleteApartmentId').value;
    
    fetch(`/api/apartments/${apartmentId}`, {
        method: 'DELETE'
    })
    .then(response => {
        if (response.ok) {
            return response.text();
        } else {
            throw new Error('Failed to delete apartment');
        }
    })
    .then(message => {
        alert('Apartment deleted successfully');
        bootstrap.Modal.getInstance(document.getElementById('deleteConfirmModal')).hide();
        loadOwnerApartments();
    })
    .catch(error => {
        console.error('Error:', error);
        alert('Error: ' + error.message);
    });
};
