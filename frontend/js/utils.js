// js/utils.js

// Base de la API, relativa a la página (sin "/" delante): así funciona igual abriendo el
// frontend directamente (/api/v1/...) que a través del hub (/inventario/api/v1/...).
export const API_BASE = 'api/v1';

// Estados de un activo (enum EstatActiu del backend). Único sitio donde se definen etiqueta y colores.
export const ESTATS_ACTIU = {
    EN_US: { label: 'EN ÚS', badge: 'bg-success', text: 'text-success' },
    STOCK: { label: 'STOCK', badge: 'bg-secondary', text: 'text-secondary' },
    REPARACIO: { label: 'REPARACIÓ', badge: 'bg-warning text-dark', text: 'text-warning' },
    BAIXA: { label: 'BAIXA', badge: 'bg-danger', text: 'text-danger' }
};

// Estados de una persona (enum EstatUsuari del backend)
export const ESTATS_USUARI = {
    ACTIU: { label: 'ACTIU', badge: 'bg-success', text: '' },
    BAIXA: { label: 'BAIXA', badge: 'bg-danger', text: 'text-muted text-decoration-line-through' }
};

/**
 * Escapa un valor antes de meterlo en HTML. Evita que unas notas con comillas o "<"
 * rompan la tabla o el tooltip.
 */
export function escapeHtml(value) {
    if (value === null || value === undefined) return '';
    return String(value)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

/**
 * Rellena un <select> con las opciones de un mapa de estados (ESTATS_ACTIU / ESTATS_USUARI)
 */
export function fillEstatSelect($select, estats) {
    $select.empty();
    $.each(estats, function (value, estat) {
        $select.append(`<option value="${value}">${estat.label}</option>`);
    });
}

/**
 * Mensaje de error legible: el backend devuelve un Problem Detail con el motivo en "detail"
 * (p. ej. "El activo 1570 ya está asignado a mlopez").
 */
export function errorMessage(xhr, fallback) {
    return (xhr.responseJSON && xhr.responseJSON.detail) || fallback;
}

/**
 * Carga datos desde una API y renderiza las filas en un tbody
 * @param {string} url - Endpoint de la API
 * @param {jQuery} $tbody - Referencia al cuerpo de la tabla
 * @param {function} renderRowCallback - Función que define cómo dibujar cada fila
 * @param {string} query - Término de búsqueda opcional
 */
export function fetchAndRender(url, $tbody, renderRowCallback, query = '') {
    // Calculamos el colspan dinámicamente basándonos en las columnas del thead
    const columnCount = $tbody.closest('table').find('thead th').length || 7;
    $tbody.html(`<tr><td colspan="${columnCount}" class="text-center">Cargando...</td></tr>`);

    const finalUrl = query ? `${url}?search=${encodeURIComponent(query)}` : url;

    $.ajax({
        url: finalUrl,
        method: 'GET',
        dataType: 'json',
        success: function (datos) {
            $tbody.empty();
            if (datos.length === 0) {
                $tbody.html(`<tr><td colspan="${columnCount}" class="text-center">No se encontraron resultados</td></tr>`);
                return;
            }
            $.each(datos, function (i, item) {
                $tbody.append(renderRowCallback(item));
            });
        },
        error: function (xhr, status, error) {
            console.error("Error API:", error);
            $tbody.html(`<tr><td colspan="${columnCount}" class="text-danger text-center">Error al conectar con el servidor</td></tr>`);
        }
    });
}
