// js/utils.js

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