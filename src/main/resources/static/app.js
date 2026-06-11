/* ═══════════════════════════════════════════════════════════
   STOREVAULT — APP.JS
   Full CRUD client for Spring Boot REST API
═══════════════════════════════════════════════════════════ */

const API = '/api/products';
const PAGE_SIZE = 6;

// ─── State ────────────────────────────────────────────────────────────────────
let allProducts    = [];
let filteredList   = [];
let currentPage    = 1;
let deleteTargetId = null;
let searchDebounce = null;

// ─── DOM refs ─────────────────────────────────────────────────────────────────
const $tbody         = document.getElementById('product-tbody');
const $emptyState    = document.getElementById('empty-state');
const $loadingState  = document.getElementById('loading-state');
const $pagination    = document.getElementById('pagination');
const $searchInput   = document.getElementById('search-input');
const $searchClear   = document.getElementById('search-clear');
const $categoryFilter= document.getElementById('category-filter');
const $toastContainer= document.getElementById('toast-container');

// Modal
const $modalOverlay  = document.getElementById('modal-overlay');
const $modalTitle    = document.getElementById('modal-title');
const $productForm   = document.getElementById('product-form');
const $productId     = document.getElementById('product-id');
const $btnSubmitText = document.getElementById('btn-submit-text');

// Delete modal
const $deleteOverlay = document.getElementById('delete-overlay');
const $deleteNameEl  = document.getElementById('delete-product-name');

// Sidebar
const $sidebar  = document.getElementById('sidebar');
const $menuBtn  = document.getElementById('menu-btn');
// ═══════════════════════════════════════════════════════════
//  API HELPERS
// ═══════════════════════════════════════════════════════════
async function apiFetch(url, options = {}) {
  const token = localStorage.getItem('token');
  const headers = { 'Content-Type': 'application/json' };
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }
  const defaults = { headers };

  const res = await fetch(url, { ...defaults, ...options });
  if ((res.status === 401 || res.status === 403) && !url.includes('/api/auth/')) {
    logout();
    throw new Error('Authentication expired or required. Please sign in again.');
  }
  if (!res.ok) {
    const contentType = res.headers.get('content-type');
    let errMsg = `HTTP ${res.status}`;
    if (contentType && contentType.includes('application/json')) {
      const err = await res.json().catch(() => ({}));
      errMsg = err.error || err.message || errMsg;
    } else {
      const text = await res.text().catch(() => '');
      if (text) errMsg = text;
    }
    throw new Error(errMsg);
  }
  if (res.status === 204) return null;
  return res.json();
}
const api = {
  getAll:      ()         => apiFetch(API),
  getById:     (id)       => apiFetch(`${API}/${id}`),
  getStats:    ()         => apiFetch(`${API}/stats`),
  getCategories: ()       => apiFetch(`${API}/categories`),
  create:      (data)     => apiFetch(API, { method: 'POST', body: JSON.stringify(data) }),
  update:      (id, data) => apiFetch(`${API}/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
  delete:      (id)       => apiFetch(`${API}/${id}`, { method: 'DELETE' }),
  // Advanced
  getByPriceRange: (min, max) => apiFetch(`${API}/price-range?min=${min}&max=${max}`),
  getInvValue:     (cat)      => apiFetch(`${API}/inventory-value?category=${encodeURIComponent(cat)}`),
  bulkUpdate:      (cat, pct) => apiFetch(`${API}/update-prices?category=${encodeURIComponent(cat)}&percentage=${pct}`, { method: 'PATCH' }),
  
  // Scheduler APIs
  getSchedulerConfig:    ()     => apiFetch('/api/scheduler/config'),
  updateSchedulerConfig: (data) => apiFetch('/api/scheduler/config', { method: 'POST', body: JSON.stringify(data) }),
  triggerScheduler:      ()     => apiFetch('/api/scheduler/trigger', { method: 'POST' }),
  getSchedulerLogs:      ()     => apiFetch('/api/scheduler/logs'),
  clearSchedulerLogs:    ()     => apiFetch('/api/scheduler/logs', { method: 'DELETE' }),

  // Auth APIs
  login:    (email, password) => apiFetch('/api/auth/login', { method: 'POST', body: JSON.stringify({ email, password }) }),
  register: (username, email, password) => apiFetch('/api/auth/register', { method: 'POST', body: JSON.stringify({ username, email, password }) }),
};

// ═══════════════════════════════════════════════════════════
//  FETCH & RENDER
// ═══════════════════════════════════════════════════════════
async function loadProducts() {
  showLoading(true);
  try {
    const [products, stats, categories] = await Promise.all([
      api.getAll(),
      api.getStats(),
      api.getCategories(),
    ]);
    allProducts = products;
    updateStats(stats);
    populateCategoryFilter(categories);
    applyFilters();
  } catch (err) {
    showToast('Failed to load products: ' + err.message, 'error');
  } finally {
    showLoading(false);
  }
}

function applyFilters() {
  const search   = $searchInput.value.trim().toLowerCase();
  const category = $categoryFilter.value;
  const minPrice = parseFloat(document.getElementById('min-price').value) || 0;
  const maxPrice = parseFloat(document.getElementById('max-price').value) || Infinity;

  filteredList = allProducts.filter(p => {
    const matchSearch = !search ||
      p.name.toLowerCase().includes(search) ||
      (p.description || '').toLowerCase().includes(search);
    const matchCat = !category || p.category === category;
    const matchPrice = p.price >= minPrice && p.price <= maxPrice;
    return matchSearch && matchCat && matchPrice;
  });

  // Handle Category Insights (Native SQL Demo)
  const $insights = document.getElementById('category-insights');
  if (category) {
    $insights.style.display = 'flex';
    fetchInventoryValue(category);
  } else {
    $insights.style.display = 'none';
  }

  currentPage = 1;
  renderTable();
}

async function fetchInventoryValue(category) {
  const $valEl = document.getElementById('cat-inv-val');
  $valEl.textContent = '...';
  try {
    const res = await api.getInvValue(category);
    $valEl.textContent = `$${parseFloat(res.totalInventoryValue).toLocaleString(undefined, {minimumFractionDigits: 2})}`;
  } catch (err) {
    $valEl.textContent = 'Error';
  }
}

function renderTable() {
  const start = (currentPage - 1) * PAGE_SIZE;
  const page  = filteredList.slice(start, start + PAGE_SIZE);

  $tbody.innerHTML = '';

  if (filteredList.length === 0) {
    $emptyState.style.display = 'flex';
    document.getElementById('product-table').style.display = 'none';
    $pagination.innerHTML = '';
    return;
  }

  $emptyState.style.display = 'none';
  document.getElementById('product-table').style.display = '';

  page.forEach((p, i) => {
    const row = document.createElement('tr');
    row.innerHTML = `
      <td style="color:var(--text-muted);font-size:13px">${start + i + 1}</td>
      <td>
        <div class="product-cell">
          ${p.imageUrl
            ? `<img src="${escHtml(p.imageUrl)}" alt="${escHtml(p.name)}" class="product-thumb" onerror="this.src='data:image/svg+xml,%3Csvg xmlns=%22http://www.w3.org/2000/svg%22 viewBox=%220 0 40 40%22%3E%3Crect width=%2240%22 height=%2240%22 fill=%22${document.documentElement.getAttribute('data-theme') === 'light' ? '%23f1f5f9' : '%231a1d2b'}%22/%3E%3Ctext x=%2250%25%22 y=%2255%25%22 dominant-baseline=%22middle%22 text-anchor=%22middle%22 fill=%22%23475569%22 font-size=%2218%22%3E%F0%9F%93%A6%3C/text%3E%3C/svg%3E'" />`
            : `<div class="product-thumb" style="display:flex;align-items:center;justify-content:center;font-size:18px">📦</div>`
          }
          <div>
            <div class="product-name">${escHtml(p.name)}</div>
            <div class="product-desc">${escHtml(p.description || '—')}</div>
          </div>
        </div>
      </td>
      <td><span class="category-pill">${escHtml(p.category)}</span></td>
      <td style="font-weight:600">$${parseFloat(p.price).toFixed(2)}</td>
      <td>${stockBadge(p.stockQuantity)}</td>
      <td style="color:var(--text-muted);font-size:12px">${formatDate(p.createdAt)}</td>
      <td>
        <div class="actions-cell">
          <button class="action-btn edit" title="Edit" onclick="openEditModal(${p.id})">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M11 4H4a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 013 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>
          </button>
          <button class="action-btn delete" title="Delete" onclick="openDeleteModal(${p.id}, '${escHtml(p.name).replace(/'/g, "\\'")}')">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6l-1 14a2 2 0 01-2 2H8a2 2 0 01-2-2L5 6"/><path d="M10 11v6M14 11v6"/><path d="M9 6V4h6v2"/></svg>
          </button>
        </div>
      </td>`;
    $tbody.appendChild(row);
  });

  renderPagination();
}

function renderPagination() {
  const totalPages = Math.ceil(filteredList.length / PAGE_SIZE);
  $pagination.innerHTML = '';
  if (totalPages <= 1) return;

  const mkBtn = (label, page, active = false) => {
    const btn = document.createElement('button');
    btn.className = 'page-btn' + (active ? ' active' : '');
    btn.textContent = label;
    btn.onclick = () => { currentPage = page; renderTable(); };
    return btn;
  };

  if (currentPage > 1)     $pagination.appendChild(mkBtn('‹', currentPage - 1));
  for (let i = 1; i <= totalPages; i++) {
    $pagination.appendChild(mkBtn(i, i, i === currentPage));
  }
  if (currentPage < totalPages) $pagination.appendChild(mkBtn('›', currentPage + 1));
}

// ═══════════════════════════════════════════════════════════
//  STATS & CATEGORIES
// ═══════════════════════════════════════════════════════════
function updateStats(stats) {
  animateCount('stat-total-val', stats.totalProducts);
  animateCount('stat-cats-val',  stats.totalCategories);
  animateCount('stat-low-val',   stats.lowStockCount);
}

function animateCount(id, target) {
  const el = document.getElementById(id);
  let current = 0;
  const step = Math.ceil(target / 20) || 1;
  const interval = setInterval(() => {
    current = Math.min(current + step, target);
    el.textContent = current;
    if (current >= target) clearInterval(interval);
  }, 40);
}

function populateCategoryFilter(categories) {
  const current = $categoryFilter.value;
  // keep "All Categories" option
  while ($categoryFilter.options.length > 1) $categoryFilter.remove(1);
  categories.forEach(cat => {
    const opt = document.createElement('option');
    opt.value = opt.textContent = cat;
    $categoryFilter.appendChild(opt);
  });
  if (current) $categoryFilter.value = current;
}

// ═══════════════════════════════════════════════════════════
//  MODAL — CREATE / EDIT
// ═══════════════════════════════════════════════════════════
function openCreateModal() {
  $productId.value = '';
  $productForm.reset();
  clearFormErrors();
  $modalTitle.textContent = 'Add Product';
  $btnSubmitText.textContent = 'Save Product';
  $modalOverlay.classList.add('open');
}

async function openEditModal(id) {
  clearFormErrors();
  $modalTitle.textContent = 'Edit Product';
  $btnSubmitText.textContent = 'Update Product';
  $modalOverlay.classList.add('open');

  try {
    const p = await api.getById(id);
    $productId.value        = p.id;
    document.getElementById('f-name').value     = p.name || '';
    document.getElementById('f-desc').value     = p.description || '';
    document.getElementById('f-price').value    = p.price;
    document.getElementById('f-stock').value    = p.stockQuantity;
    document.getElementById('f-category').value = p.category || '';
    document.getElementById('f-image').value    = p.imageUrl || '';
  } catch (err) {
    showToast('Could not load product: ' + err.message, 'error');
    closeModal();
  }
}

function closeModal() {
  $modalOverlay.classList.remove('open');
}

// ─── Form submit ───────────────────────────────────────────
$productForm.addEventListener('submit', async (e) => {
  e.preventDefault();
  if (!validateForm()) return;

  const id = $productId.value;
  const data = {
    name:          document.getElementById('f-name').value.trim(),
    description:   document.getElementById('f-desc').value.trim(),
    price:         parseFloat(document.getElementById('f-price').value),
    stockQuantity: parseInt(document.getElementById('f-stock').value, 10),
    category:      document.getElementById('f-category').value.trim(),
    imageUrl:      document.getElementById('f-image').value.trim() || null,
  };

  const $submitBtn = document.getElementById('btn-submit');
  $submitBtn.disabled = true;
  $btnSubmitText.textContent = id ? 'Updating…' : 'Saving…';

  try {
    if (id) {
      await api.update(id, data);
      showToast('Product updated successfully!', 'success');
    } else {
      await api.create(data);
      showToast('Product created successfully!', 'success');
    }
    $productForm.reset();
    $productId.value = '';
    closeModal();
    await loadProducts();
  } catch (err) {
    showToast('Error: ' + err.message, 'error');
  } finally {
    $submitBtn.disabled = false;
    $btnSubmitText.textContent = id ? 'Update Product' : 'Save Product';
  }
});

// ─── Validation ────────────────────────────────────────────
function validateForm() {
  clearFormErrors();
  let valid = true;

  const name  = document.getElementById('f-name').value.trim();
  const price = document.getElementById('f-price').value;
  const stock = document.getElementById('f-stock').value;
  const cat   = document.getElementById('f-category').value.trim();

  if (!name || name.length < 2) {
    setError('f-name', 'err-name', 'Name must be at least 2 characters.');
    valid = false;
  }
  if (!price || parseFloat(price) <= 0) {
    setError('f-price', 'err-price', 'Price must be greater than 0.');
    valid = false;
  }
  if (stock === '' || parseInt(stock, 10) < 0) {
    setError('f-stock', 'err-stock', 'Stock quantity cannot be negative.');
    valid = false;
  }
  if (!cat) {
    setError('f-category', 'err-category', 'Category is required.');
    valid = false;
  }
  return valid;
}

function setError(fieldId, errId, msg) {
  document.getElementById(fieldId).classList.add('error');
  document.getElementById(errId).textContent = msg;
}
function clearFormErrors() {
  ['f-name','f-price','f-stock','f-category'].forEach(id => {
    document.getElementById(id).classList.remove('error');
  });
  ['err-name','err-price','err-stock','err-category','err-desc','err-image'].forEach(id => {
    const el = document.getElementById(id);
    if (el) el.textContent = '';
  });
}

// ═══════════════════════════════════════════════════════════
//  DELETE MODAL
// ═══════════════════════════════════════════════════════════
function openDeleteModal(id, name) {
  deleteTargetId = id;
  $deleteNameEl.textContent = name;
  $deleteOverlay.classList.add('open');
}
function closeDeleteModal() {
  deleteTargetId = null;
  $deleteOverlay.classList.remove('open');
}

document.getElementById('delete-confirm').addEventListener('click', async () => {
  if (!deleteTargetId) return;
  const btn = document.getElementById('delete-confirm');
  btn.disabled = true;
  btn.textContent = 'Deleting…';
  try {
    await api.delete(deleteTargetId);
    showToast('Product deleted.', 'success');
    closeDeleteModal();
    await loadProducts();
  } catch (err) {
    showToast('Delete failed: ' + err.message, 'error');
  } finally {
    btn.disabled = false;
    btn.textContent = 'Delete';
  }
});

// ═══════════════════════════════════════════════════════════
//  TOAST
// ═══════════════════════════════════════════════════════════
function showToast(message, type = 'success') {
  const toast = document.createElement('div');
  toast.className = `toast ${type}`;
  toast.innerHTML = `<span class="toast-dot"></span><span>${escHtml(message)}</span>`;
  $toastContainer.appendChild(toast);
  setTimeout(() => {
    toast.classList.add('hide');
    setTimeout(() => toast.remove(), 350);
  }, 3500);
}

// ═══════════════════════════════════════════════════════════
//  UTILITIES
// ═══════════════════════════════════════════════════════════
function showLoading(visible) {
  $loadingState.style.display = visible ? 'flex' : 'none';
  if (visible) {
    $emptyState.style.display = 'none';
    document.getElementById('product-table').style.display = 'none';
  }
}

function stockBadge(qty) {
  if (qty === 0)   return `<span class="stock-badge low">● Out of stock</span>`;
  if (qty < 10)    return `<span class="stock-badge low">⚠ ${qty} left</span>`;
  if (qty < 25)    return `<span class="stock-badge warn">● ${qty}</span>`;
  return             `<span class="stock-badge ok">● ${qty}</span>`;
}

function formatDate(isoStr) {
  if (!isoStr) return '—';
  const d = new Date(isoStr);
  return d.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
}

function escHtml(str) {
  if (!str) return '';
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

// ═══════════════════════════════════════════════════════════
//  EVENT BINDINGS
// ═══════════════════════════════════════════════════════════

// Open create modal
document.getElementById('btn-add-product').addEventListener('click', openCreateModal);

// Close modals
document.getElementById('modal-close').addEventListener('click', closeModal);
document.getElementById('btn-cancel').addEventListener('click', closeModal);
$modalOverlay.addEventListener('click', (e) => { if (e.target === $modalOverlay) closeModal(); });

document.getElementById('delete-close').addEventListener('click', closeDeleteModal);
document.getElementById('delete-cancel').addEventListener('click', closeDeleteModal);
$deleteOverlay.addEventListener('click', (e) => { if (e.target === $deleteOverlay) closeDeleteModal(); });

// Keyboard ESC
document.addEventListener('keydown', (e) => {
  if (e.key === 'Escape') { closeModal(); closeDeleteModal(); }
});

// Search with debounce
$searchInput.addEventListener('input', () => {
  $searchClear.style.display = $searchInput.value ? 'block' : 'none';
  clearTimeout(searchDebounce);
  searchDebounce = setTimeout(applyFilters, 300);
});
$searchClear.addEventListener('click', () => {
  $searchInput.value = '';
  $searchClear.style.display = 'none';
  applyFilters();
});

// Category filter
$categoryFilter.addEventListener('change', applyFilters);

// Price filters
document.getElementById('min-price').addEventListener('input', applyFilters);
document.getElementById('max-price').addEventListener('input', applyFilters);

// ═══════════════════════════════════════════════════════════
//  BULK UPDATE
// ═══════════════════════════════════════════════════════════
const $bulkOverlay = document.getElementById('bulk-overlay');
const $bulkForm    = document.getElementById('bulk-form');

document.getElementById('btn-bulk-update').addEventListener('click', () => {
  const cat = $categoryFilter.value;
  if (!cat) return;
  document.getElementById('bulk-cat-name').textContent = cat;
  $bulkOverlay.classList.add('open');
});

document.getElementById('bulk-close').addEventListener('click', () => $bulkOverlay.classList.remove('open'));
document.getElementById('bulk-cancel').addEventListener('click', () => $bulkOverlay.classList.remove('open'));

$bulkForm.addEventListener('submit', async (e) => {
  e.preventDefault();
  const cat = $categoryFilter.value;
  const pct = parseFloat(document.getElementById('bulk-percentage').value);
  if (isNaN(pct)) return;

  const btn = document.getElementById('bulk-submit');
  btn.disabled = true;
  btn.textContent = 'Updating...';

  try {
    const res = await api.bulkUpdate(cat, pct);
    showToast(`${res.updatedCount} products updated in ${cat}`, 'success');
    $bulkOverlay.classList.remove('open');
    $bulkForm.reset();
    await loadProducts();
  } catch (err) {
    showToast('Bulk update failed: ' + err.message, 'error');
  } finally {
    btn.disabled = false;
    btn.textContent = 'Apply Update';
  }
});

// Sidebar toggle (mobile)
$menuBtn.addEventListener('click', () => $sidebar.classList.toggle('open'));

// ═══════════════════════════════════════════════════════════
//  THEME TOGGLE
// ═══════════════════════════════════════════════════════════
const $themeToggle = document.getElementById('theme-toggle');
const $sunIcon     = $themeToggle.querySelector('.sun-icon');
const $moonIcon    = $themeToggle.querySelector('.moon-icon');

function initTheme() {
  const savedTheme = localStorage.getItem('theme') || 'dark';
  setTheme(savedTheme);
}

function setTheme(theme) {
  document.documentElement.setAttribute('data-theme', theme);
  localStorage.setItem('theme', theme);
  
  if (theme === 'light') {
    $sunIcon.style.display = 'none';
    $moonIcon.style.display = 'block';
  } else {
    $sunIcon.style.display = 'block';
    $moonIcon.style.display = 'none';
  }
}

$themeToggle.addEventListener('click', () => {
  const current = document.documentElement.getAttribute('data-theme');
  setTheme(current === 'light' ? 'dark' : 'light');
});

// ═══════════════════════════════════════════════════════════
//  TAB NAVIGATION & SCHEDULER CONTROLLER
// ═══════════════════════════════════════════════════════════
let activeTab = 'products';
let logPollInterval = null;

const $navProducts = document.getElementById('nav-products');
const $navScheduler = document.getElementById('nav-scheduler');
const $productsPanel = document.getElementById('products-panel');
const $schedulerPanel = document.getElementById('scheduler-panel');
const $btnAddProduct = document.getElementById('btn-add-product');

function switchTab(tab) {
  activeTab = tab;
  if (tab === 'products') {
    $navProducts.classList.add('active');
    $navScheduler.classList.remove('active');
    $productsPanel.style.display = '';
    $schedulerPanel.style.display = 'none';
    $btnAddProduct.style.display = '';
    document.querySelector('.page-title').textContent = 'Product Inventory';
    document.querySelector('.page-sub').textContent = 'Manage your store catalogue';
    
    clearInterval(logPollInterval);
    logPollInterval = null;
  } else {
    $navProducts.classList.remove('active');
    $navScheduler.classList.add('active');
    $productsPanel.style.display = 'none';
    $schedulerPanel.style.display = 'block';
    $btnAddProduct.style.display = 'none';
    document.querySelector('.page-title').textContent = 'Automation & Scheduler';
    document.querySelector('.page-sub').textContent = 'Dynamic task scheduling and logs';
    
    loadSchedulerConfig();
    loadSchedulerLogs();
    clearInterval(logPollInterval);
    logPollInterval = setInterval(loadSchedulerLogs, 3000);
  }
}

// Translate cron expression to UI controls
function cronToUi(cron) {
  cron = (cron || '').trim();
  const parts = cron.split(/\s+/);
  
  const $mode = document.getElementById('scheduler-mode');
  const $time = document.getElementById('scheduler-time');
  const $minute = document.getElementById('scheduler-minute');
  const $cron = document.getElementById('scheduler-cron');
  
  // Default values
  $time.value = '12:00';
  $minute.value = '0';
  $cron.value = cron;
  
  if (cron === '*/10 * * * * *') {
    $mode.value = '10s';
  } else if (cron === '0 * * * * *') {
    $mode.value = 'minute';
  } else if (parts.length === 6 && parts[0] === '0' && parts[1] !== '*' && parts[2] === '*' && parts[3] === '*' && parts[4] === '*' && parts[5] === '*') {
    $mode.value = 'hourly';
    $minute.value = parts[1];
  } else if (parts.length === 6 && parts[0] === '0' && parts[1] !== '*' && parts[2] !== '*' && parts[3] === '*' && parts[4] === '*' && parts[5] === '*') {
    $mode.value = 'daily';
    const hour = parts[2].padStart(2, '0');
    const min = parts[1].padStart(2, '0');
    $time.value = `${hour}:${min}`;
  } else {
    $mode.value = 'custom';
  }
  
  updateSchedulerUiVisibility();
}

// Translate UI controls to cron expression
function uiToCron() {
  const mode = document.getElementById('scheduler-mode').value;
  const timeVal = document.getElementById('scheduler-time').value || '12:00';
  const minuteVal = parseInt(document.getElementById('scheduler-minute').value, 10) || 0;
  const cronVal = document.getElementById('scheduler-cron').value.trim();
  
  if (mode === '10s') {
    return '*/10 * * * * *';
  } else if (mode === 'minute') {
    return '0 * * * * *';
  } else if (mode === 'hourly') {
    return `0 ${minuteVal} * * * *`;
  } else if (mode === 'daily') {
    const [hour, min] = timeVal.split(':');
    const h = parseInt(hour, 10);
    const m = parseInt(min, 10);
    return `0 ${m} ${h} * * *`;
  } else {
    return cronVal;
  }
}

// Show/hide time or custom cron controls based on frequency
function updateSchedulerUiVisibility() {
  const mode = document.getElementById('scheduler-mode').value;
  
  const $timeContainer = document.getElementById('scheduler-time-container');
  const $minuteContainer = document.getElementById('scheduler-minute-container');
  const $cronContainer = document.getElementById('scheduler-cron-container');
  
  $timeContainer.style.display = (mode === 'daily') ? 'flex' : 'none';
  $minuteContainer.style.display = (mode === 'hourly') ? 'flex' : 'none';
  $cronContainer.style.display = (mode === 'custom') ? 'flex' : 'none';
}

async function loadSchedulerConfig() {
  try {
    const config = await api.getSchedulerConfig();
    document.getElementById('scheduler-enabled').checked = config.enabled;
    cronToUi(config.cronExpression);
  } catch (err) {
    showToast('Failed to load scheduler configuration: ' + err.message, 'error');
  }
}

async function loadSchedulerLogs() {
  const $logsConsole = document.getElementById('logs-console');
  try {
    const logs = await api.getSchedulerLogs();
    if (logs.length === 0) {
      $logsConsole.innerHTML = '<div style="color: var(--text-muted); font-style: italic;">No logs available yet.</div>';
      return;
    }
    $logsConsole.innerHTML = logs.map(log => {
      let colorClass = '';
      if (log.includes('ERROR:')) colorClass = 'color: #f87171;';
      else if (log.includes('WARNING:')) colorClass = 'color: #fbbf24;';
      else if (log.includes('SUCCESS:')) colorClass = 'color: #34d399;';
      return `<div style="${colorClass}">${escHtml(log)}</div>`;
    }).join('');
    $logsConsole.scrollTop = $logsConsole.scrollHeight;
  } catch (err) {
    $logsConsole.innerHTML = `<div style="color: #f87171;">Failed to load logs: ${escHtml(err.message)}</div>`;
  }
}

// Event bindings
$navProducts.addEventListener('click', (e) => { e.preventDefault(); switchTab('products'); });
$navScheduler.addEventListener('click', (e) => { e.preventDefault(); switchTab('scheduler'); });

document.getElementById('scheduler-mode').addEventListener('change', updateSchedulerUiVisibility);

document.getElementById('scheduler-form').addEventListener('submit', async (e) => {
  e.preventDefault();
  const enabled = document.getElementById('scheduler-enabled').checked;
  const cronExpression = uiToCron();
  
  const btn = document.getElementById('btn-save-scheduler');
  btn.disabled = true;
  btn.textContent = 'Saving...';
  
  try {
    await api.updateSchedulerConfig({ enabled, cronExpression });
    showToast('Scheduler configuration saved successfully!', 'success');
    await loadSchedulerLogs();
  } catch (err) {
    showToast('Failed to save scheduler settings: ' + err.message, 'error');
  } finally {
    btn.disabled = false;
    btn.textContent = 'Save Settings';
  }
});

document.getElementById('btn-trigger-scheduler').addEventListener('click', async () => {
  const btn = document.getElementById('btn-trigger-scheduler');
  btn.disabled = true;
  btn.textContent = 'Running...';
  
  try {
    await api.triggerScheduler();
    showToast('Scheduled task triggered successfully!', 'success');
    await loadSchedulerLogs();
  } catch (err) {
    showToast('Failed to trigger scheduler: ' + err.message, 'error');
  } finally {
    btn.disabled = false;
    btn.textContent = 'Run Check Now';
  }
});

document.getElementById('btn-clear-logs').addEventListener('click', async () => {
  try {
    await api.clearSchedulerLogs();
    showToast('Logs cleared.', 'success');
    await loadSchedulerLogs();
  } catch (err) {
    showToast('Failed to clear logs: ' + err.message, 'error');
  }
});

// ═══════════════════════════════════════════════════════════
//  AUTHENTICATION SESSION HANDLERS
// ═══════════════════════════════════════════════════════════
const $authView     = document.getElementById('auth-view');
const $loginForm    = document.getElementById('login-form');
const $registerForm = document.getElementById('register-form');
const $toRegister   = document.getElementById('to-register');
const $toLogin      = document.getElementById('to-login');
const $btnLogout    = document.getElementById('btn-logout');

function logout() {
  localStorage.removeItem('token');
  $authView.style.display = 'flex';
  document.getElementById('sidebar').style.display = 'none';
  document.querySelector('.main-content').style.display = 'none';
  clearInterval(logPollInterval);
  logPollInterval = null;
}

function checkAuth() {
  const token = localStorage.getItem('token');
  if (!token) {
    $authView.style.display = 'flex';
    document.getElementById('sidebar').style.display = 'none';
    document.querySelector('.main-content').style.display = 'none';
  } else {
    $authView.style.display = 'none';
    document.getElementById('sidebar').style.display = '';
    document.querySelector('.main-content').style.display = '';
    switchTab('products');
    loadProducts();
  }
}

// Auth toggle views
$toRegister.addEventListener('click', (e) => {
  e.preventDefault();
  $loginForm.style.display = 'none';
  $registerForm.style.display = 'block';
  document.getElementById('auth-title').textContent = 'Create Account';
  document.getElementById('auth-subtitle').textContent = 'Register to start managing your warehouse';
});

$toLogin.addEventListener('click', (e) => {
  e.preventDefault();
  $loginForm.style.display = 'block';
  $registerForm.style.display = 'none';
  document.getElementById('auth-title').textContent = 'Welcome to StoreVault';
  document.getElementById('auth-subtitle').textContent = 'Please sign in to access your inventory';
});

// Auth form submissions
$loginForm.addEventListener('submit', async (e) => {
  e.preventDefault();
  const email = document.getElementById('login-email').value.trim();
  const password = document.getElementById('login-password').value;
  
  const submitBtn = $loginForm.querySelector('button[type="submit"]');
  submitBtn.disabled = true;
  submitBtn.textContent = 'Signing In...';
  
  try {
    const res = await api.login(email, password);
    localStorage.setItem('token', res.token);
    showToast('Signed in successfully!', 'success');
    $loginForm.reset();
    checkAuth();
  } catch (err) {
    showToast('Login failed: ' + err.message, 'error');
  } finally {
    submitBtn.disabled = false;
    submitBtn.textContent = 'Sign In';
  }
});

$registerForm.addEventListener('submit', async (e) => {
  e.preventDefault();
  const username = document.getElementById('register-username').value.trim();
  const email = document.getElementById('register-email').value.trim();
  const password = document.getElementById('register-password').value;
  
  const submitBtn = $registerForm.querySelector('button[type="submit"]');
  submitBtn.disabled = true;
  submitBtn.textContent = 'Registering...';
  
  try {
    const res = await api.register(username, email, password);
    localStorage.setItem('token', res.token);
    showToast('Account created successfully!', 'success');
    $registerForm.reset();
    checkAuth();
  } catch (err) {
    showToast('Registration failed: ' + err.message, 'error');
  } finally {
    submitBtn.disabled = false;
    submitBtn.textContent = 'Create Account';
  }
});

$btnLogout.addEventListener('click', (e) => {
  e.preventDefault();
  logout();
});

// ═══════════════════════════════════════════════════════════
//  INIT
// ═══════════════════════════════════════════════════════════
initTheme();
checkAuth();
