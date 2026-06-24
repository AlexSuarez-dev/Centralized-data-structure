// navbar.js
// Componente de navegación compartido entre todas las páginas del inventario.
// Se inyecta en el elemento <div id="navbar-container"></div> de cada página.

const NAV_ITEMS = [
    { href: 'index.html', label: 'Ordenadores', icon: 'bi-pc-display' },
    { href: 'usuaris.html', label: 'Usuaris', icon: 'bi-people-fill' },
    { href: 'pinpads.html', label: 'Pinpads', icon: 'bi-credit-card' },
    { href: 'impressores.html', label: 'Impressores', icon: 'bi-printer-fill' }
];

function nombreArchivoActual() {
    const path = window.location.pathname;
    const archivo = path.substring(path.lastIndexOf('/') + 1);
    return archivo === '' ? 'index.html' : archivo;
}

function renderNavbar() {
    const actual = nombreArchivoActual();

    const links = NAV_ITEMS.map(item => {
        const activo = item.href === actual ? ' active' : '';
        const ariaCurrent = item.href === actual ? ' aria-current="page"' : '';
        return `
            <li class="nav-item">
                <a class="nav-link${activo}" href="${item.href}"${ariaCurrent}>
                    <i class="bi ${item.icon} me-1"></i>${item.label}
                </a>
            </li>`;
    }).join('');

    const html = `
    <nav class="navbar navbar-expand-lg navbar-dark bg-dark mb-4">
        <div class="container-fluid px-4">
            <a class="navbar-brand fw-semibold" href="index.html">
                <i class="bi bi-building me-2"></i>Inventario · Barcelona Turisme
            </a>
            <button class="navbar-toggler" type="button" data-bs-toggle="collapse"
                data-bs-target="#navbarInventario" aria-controls="navbarInventario"
                aria-expanded="false" aria-label="Mostrar navegación">
                <span class="navbar-toggler-icon"></span>
            </button>
            <div class="collapse navbar-collapse" id="navbarInventario">
                <ul class="navbar-nav ms-auto mb-2 mb-lg-0">
                    ${links}
                </ul>
            </div>
        </div>
    </nav>`;

    const container = document.getElementById('navbar-container');
    if (container) {
        container.innerHTML = html;
    } else {
        // Fallback: si la página no tiene el contenedor, lo insertamos al principio del body
        document.body.insertAdjacentHTML('afterbegin', html);
    }
}

document.addEventListener('DOMContentLoaded', renderNavbar);

export { renderNavbar };
