// FoodDash single-page frontend. Plain JavaScript, no build step.
// It only ever calls relative URLs like /api/restaurants; nginx forwards them to the API gateway.
(() => {
    'use strict';

    const app = document.getElementById('app');
    const cartCount = document.getElementById('cart-count');
    const toastEl = document.getElementById('toast');

    const FLOW = ['PLACED', 'PREPARING', 'OUT_FOR_DELIVERY', 'DELIVERED'];
    const LABELS = {
        PLACED: 'Order placed', PREPARING: 'Preparing', OUT_FOR_DELIVERY: 'Out for delivery',
        DELIVERED: 'Delivered', CANCELLED: 'Cancelled'
    };

    let pollTimer = null;
    let toastTimer = null;
    let current = { restaurant: null, items: new Map() };

    // ---------- helpers ----------
    const esc = (value) => String(value ?? '').replace(/[&<>"']/g, (c) =>
        ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
    const money = (n) => '₹' + Number(n).toFixed(2);

    async function api(path, options = {}) {
        const response = await fetch('/api' + path, {
            headers: { 'Content-Type': 'application/json' },
            ...options
        });
        const text = await response.text();
        let body = null;
        try { body = text ? JSON.parse(text) : null; } catch (e) { /* not JSON */ }
        if (!response.ok) {
            throw new Error((body && (body.error || body.message)) || `Request failed (${response.status})`);
        }
        return body;
    }

    function toast(message) {
        toastEl.textContent = message;
        toastEl.hidden = false;
        clearTimeout(toastTimer);
        toastTimer = setTimeout(() => { toastEl.hidden = true; }, 2200);
    }

    function stored(key, fallback) {
        try { return localStorage.getItem(key) ?? fallback; } catch (e) { return fallback; }
    }
    function store(key, value) {
        try { localStorage.setItem(key, value); } catch (e) { /* storage unavailable */ }
    }

    // ---------- cart (kept in the browser) ----------
    const emptyCart = () => ({ restaurantId: null, restaurantName: '', items: {} });
    function loadCart() {
        try { return JSON.parse(stored('fd_cart', '')) || emptyCart(); } catch (e) { return emptyCart(); }
    }
    let cart = loadCart();
    const cartLines = () => Object.values(cart.items);
    const subtotal = () => cartLines().reduce((sum, l) => sum + l.price * l.qty, 0);

    function saveCart() {
        store('fd_cart', JSON.stringify(cart));
        const n = cartLines().reduce((sum, l) => sum + l.qty, 0);
        cartCount.textContent = n;
        cartCount.hidden = n === 0;
    }

    function addToCart(item) {
        if (!item) return;
        if (cart.restaurantId && cart.restaurantId !== item.restaurantId) {
            if (!confirm(`Your cart has items from ${cart.restaurantName}. Start a new cart from here?`)) return;
            cart = emptyCart();
        }
        cart.restaurantId = item.restaurantId;
        cart.restaurantName = current.restaurant.name;
        const line = cart.items[item.id] || { id: item.id, name: item.name, price: Number(item.price), qty: 0 };
        line.qty = Math.min(line.qty + 1, 20);
        cart.items[item.id] = line;
        saveCart();
        toast(`${item.name} added to your cart`);
    }

    function changeQty(id, action) {
        const line = cart.items[id];
        if (!line) return;
        if (action === 'inc') line.qty = Math.min(line.qty + 1, 20);
        if (action === 'dec') line.qty -= 1;
        if (action === 'rm' || line.qty <= 0) delete cart.items[id];
        if (cartLines().length === 0) cart = emptyCart();
        saveCart();
    }

    // ---------- views ----------
    function restaurantCard(r, i) {
        return `<a class="card" href="#/restaurant/${r.id}">
            <div class="card-art tone-${i % 4}">${esc(r.emoji)}</div>
            <div class="card-body">
                <h3>${esc(r.name)}</h3>
                <p class="muted">${esc(r.cuisine)}</p>
                <div class="meta"><span class="rating">${esc(r.rating)}</span><span>${esc(r.deliveryTimeMins)} min</span></div>
            </div>
        </a>`;
    }

    async function homeView() {
        const q = new URLSearchParams(location.hash.split('?')[1] || '').get('q') || '';
        app.innerHTML = `
            <section class="hero">
                <h1>Dinner, sorted.</h1>
                <p>Pick a restaurant, build your cart, and track your order until it arrives.</p>
                <form id="search" class="search">
                    <input type="search" name="q" value="${esc(q)}" placeholder="Search for a restaurant, cuisine or dish">
                    <button class="btn" type="submit">Search</button>
                </form>
            </section>
            <h2>${q ? `Results for “${esc(q)}”` : 'Restaurants near you'}</h2>
            <div id="list" class="grid"><p class="muted">Loading…</p></div>`;
        const list = await api('/restaurants?q=' + encodeURIComponent(q));
        document.getElementById('list').innerHTML = list.length
            ? list.map(restaurantCard).join('')
            : '<div class="empty"><p>No restaurants match your search.</p></div>';
    }

    async function restaurantView(id) {
        const [restaurant, items] = await Promise.all([
            api(`/restaurants/${encodeURIComponent(id)}`),
            api(`/restaurants/${encodeURIComponent(id)}/menu`)
        ]);
        current = { restaurant, items: new Map(items.map((i) => [String(i.id), i])) };

        const groups = new Map();
        items.forEach((i) => {
            if (!groups.has(i.category)) groups.set(i.category, []);
            groups.get(i.category).push(i);
        });

        const menu = [...groups].map(([category, list]) => `
            <section class="menu-section">
                <h2>${esc(category)}</h2>
                ${list.map((i) => `
                    <div class="menu-item">
                        <div class="grow">
                            <span class="veg-dot ${i.veg ? 'veg' : 'nonveg'}" title="${i.veg ? 'Vegetarian' : 'Non-vegetarian'}"></span>
                            <strong>${esc(i.name)}</strong>
                            <div class="price">${money(i.price)}</div>
                            <p class="muted">${esc(i.description)}</p>
                        </div>
                        <button class="btn btn-sm" data-action="add" data-id="${i.id}" ${i.available ? '' : 'disabled'}>
                            ${i.available ? 'Add' : 'Sold out'}
                        </button>
                    </div>`).join('')}
            </section>`).join('');

        app.innerHTML = `
            <section class="restaurant-head">
                <div class="card-art big tone-1">${esc(restaurant.emoji)}</div>
                <div>
                    <h1>${esc(restaurant.name)}</h1>
                    <p class="muted">${esc(restaurant.cuisine)}</p>
                    <p>${esc(restaurant.description)}</p>
                    <div class="meta"><span class="rating">${esc(restaurant.rating)}</span><span>${esc(restaurant.deliveryTimeMins)} min delivery</span></div>
                </div>
            </section>
            ${menu || '<div class="empty"><p>This menu is empty.</p></div>'}`;
    }

    function cartView() {
        const lines = cartLines();
        if (lines.length === 0) {
            app.innerHTML = `<div class="narrow"><h1>Your cart</h1>
                <div class="empty"><p>Your cart is empty.</p><a class="btn" href="#/">Browse restaurants</a></div></div>`;
            return;
        }
        app.innerHTML = `
            <div class="narrow">
                <h1>Your cart</h1>
                <p class="muted">Ordering from <a href="#/restaurant/${cart.restaurantId}">${esc(cart.restaurantName)}</a></p>
                <section class="panel">
                    ${lines.map((l) => `
                        <div class="line">
                            <div class="grow"><strong>${esc(l.name)}</strong><div class="muted">${money(l.price)} each</div></div>
                            <div class="qty">
                                <button class="qty-btn" data-action="dec" data-id="${l.id}" aria-label="Remove one">−</button>
                                <span>${l.qty}</span>
                                <button class="qty-btn" data-action="inc" data-id="${l.id}" aria-label="Add one">+</button>
                            </div>
                            <div class="line-total">${money(l.price * l.qty)}</div>
                            <button class="link-btn" data-action="rm" data-id="${l.id}">Remove</button>
                        </div>`).join('')}
                    <div class="row total"><span>Item total</span><span>${money(subtotal())}</span></div>
                    <p class="muted">The delivery fee is added when you place the order.</p>
                </section>
                <form id="checkout" class="panel form">
                    <h3>Delivery details</h3>
                    <label for="customerName">Your name</label>
                    <input id="customerName" name="customerName" required value="${esc(stored('fd_name', ''))}">
                    <label for="deliveryAddress">Delivery address</label>
                    <textarea id="deliveryAddress" name="deliveryAddress" rows="3" required>${esc(stored('fd_address', ''))}</textarea>
                    <p class="muted">Payment: cash on delivery.</p>
                    <div id="checkout-error"></div>
                    <button class="btn" type="submit">Place order</button>
                </form>
            </div>`;
    }

    function timeline(order) {
        if (order.status === 'CANCELLED') return '<span class="status CANCELLED">Cancelled</span>';
        const reached = FLOW.indexOf(order.status);
        return `<ol class="timeline">${FLOW.map((s, i) =>
            `<li class="${i <= reached ? 'done' : ''}"><span class="dot"></span>${LABELS[s]}</li>`).join('')}</ol>`;
    }

    function orderHtml(order) {
        return `
            <div class="narrow">
                <h1>Order #${order.id}</h1>
                <p class="muted">${esc(order.restaurantName)} · ${esc(new Date(order.createdAt).toLocaleString())}</p>
                <section class="panel">${timeline(order)}</section>
                <section class="panel">
                    ${order.items.map((i) => `<div class="row"><span>${i.quantity} × ${esc(i.name)}</span><span>${money(i.lineTotal)}</span></div>`).join('')}
                    <div class="row"><span>Item total</span><span>${money(order.subtotal)}</span></div>
                    <div class="row"><span>Delivery fee</span><span>${money(order.deliveryFee)}</span></div>
                    <div class="row total"><span>Pay on delivery</span><span>${money(order.total)}</span></div>
                </section>
                <section class="panel"><h3>Delivering to</h3><p>${esc(order.deliveryAddress)}</p></section>
                <div class="actions"><a class="btn btn-outline" href="#/orders">All orders</a>
                    <span class="muted">Status refreshes every 5 seconds</span></div>
            </div>`;
    }

    async function orderView(id) {
        const show = async () => { app.innerHTML = orderHtml(await api(`/orders/${encodeURIComponent(id)}`)); };
        await show();
        // Re-check the status every few seconds so changes made by an admin show up on their own.
        pollTimer = setInterval(() => { show().catch(() => {}); }, 5000);
    }

    async function ordersView() {
        const name = new URLSearchParams(location.hash.split('?')[1] || '').get('name') || stored('fd_name', '');
        app.innerHTML = `
            <div class="narrow">
                <h1>My orders</h1>
                <form id="find-orders" class="panel form">
                    <label for="name">Your name (the one you used at checkout)</label>
                    <input id="name" name="name" required value="${esc(name)}">
                    <button class="btn" type="submit">Show my orders</button>
                </form>
                <div id="orders-list"></div>
            </div>`;
        if (!name) return;
        const orders = await api('/orders?customerName=' + encodeURIComponent(name));
        document.getElementById('orders-list').innerHTML = orders.length
            ? orders.map((o) => `
                <a class="panel order-row" href="#/order/${o.id}">
                    <div class="grow"><strong>${esc(o.restaurantName)}</strong>
                        <div class="muted">Order #${o.id} · ${esc(new Date(o.createdAt).toLocaleString())}</div></div>
                    <span class="status ${o.status}">${LABELS[o.status]}</span>
                    <strong>${money(o.total)}</strong>
                </a>`).join('')
            : '<div class="empty"><p>No orders found for that name.</p></div>';
    }

    // ---------- router ----------
    async function route() {
        clearInterval(pollTimer);
        const [page, arg] = location.hash.replace(/^#\//, '').split('?')[0].split('/');
        try {
            if (page === 'restaurant' && arg) await restaurantView(arg);
            else if (page === 'cart') cartView();
            else if (page === 'orders') await ordersView();
            else if (page === 'order' && arg) await orderView(arg);
            else await homeView();
        } catch (error) {
            app.innerHTML = `<div class="alert error">${esc(error.message)}</div>
                <a class="btn btn-outline" href="#/">Back to restaurants</a>`;
        }
        window.scrollTo(0, 0);
    }

    // ---------- events ----------
    document.addEventListener('click', (e) => {
        const el = e.target.closest('[data-action]');
        if (!el) return;
        const { action, id } = el.dataset;
        if (action === 'add') addToCart(current.items.get(id));
        else { changeQty(id, action); cartView(); }
    });

    document.addEventListener('submit', async (e) => {
        const form = e.target;
        if (form.id === 'search') {
            e.preventDefault();
            location.hash = '#/?q=' + encodeURIComponent(new FormData(form).get('q'));
        } else if (form.id === 'find-orders') {
            e.preventDefault();
            const name = new FormData(form).get('name').trim();
            store('fd_name', name);
            location.hash = '#/orders?name=' + encodeURIComponent(name);
            route();
        } else if (form.id === 'checkout') {
            e.preventDefault();
            const data = new FormData(form);
            const button = form.querySelector('button[type=submit]');
            button.disabled = true;
            try {
                const order = await api('/orders', {
                    method: 'POST',
                    body: JSON.stringify({
                        customerName: data.get('customerName'),
                        deliveryAddress: data.get('deliveryAddress'),
                        items: cartLines().map((l) => ({ menuItemId: l.id, quantity: l.qty }))
                    })
                });
                store('fd_name', data.get('customerName').trim());
                store('fd_address', data.get('deliveryAddress').trim());
                cart = emptyCart();
                saveCart();
                location.hash = '#/order/' + order.id;
            } catch (error) {
                document.getElementById('checkout-error').innerHTML = `<div class="alert error">${esc(error.message)}</div>`;
                button.disabled = false;
            }
        }
    });

    window.addEventListener('hashchange', route);
    saveCart();
    route();
})();
