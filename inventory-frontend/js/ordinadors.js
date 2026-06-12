import { fetchAndRender } from './utils.js';

$(document).ready(function () {
    const API_URL = 'http://localhost:8080/api/v1/ordinadors';
    const $tbody = $('#tabla-body');

    // --- 1. Lógica de renderizado específico para Ordenadores ---
    function crearFilaOrdinador(item) {
        const fecha = item.purchaseDate || 'Sin fecha';
        const obs = item.observacions || '-';

        let colorClase = '';
        if (item.estat == 1) colorClase = 'text-success';
        else if (item.estat == 2) colorClase = 'text-warning';
        else colorClase = 'text-danger';
        

        return `
            <tr>
                <td>
                    <strong class="${colorClase}">${item.nom}</strong><br>
                    <small class="text-muted">${item.model}</small>
                </td>
                <td><span class="badge bg-success">${item.type}</span></td>
                <td><small>${item.serialNumber}</small></td>
                <td>${item.ubicacio}</td>
                <td>
                    <div style="font-size: 0.85rem;">
                        <strong>SO:</strong> ${item.so || 'N/A'}<br>
                        <strong>RAM:</strong> ${item.ram || 'N/A'} | <strong>HDD:</strong> ${item.hdd || 'N/A'}
                    </div>
                </td>
                <td><small>${fecha}</small></td>
                <td><i class="text-muted" style="font-size: 0.8rem;">${obs}</i></td>
            </tr>`;
    }

    // --- 2. Evento de Búsqueda con DEBOUNCE ---
    let timer;
    $('#inventorySearch').change(function () {
        const query = $(this).val();
        clearTimeout(timer);
        timer = setTimeout(() => {
            fetchAndRender(API_URL, $tbody, crearFilaOrdinador, query);
        }, 300); // Espera 300ms antes de llamar al backend
    });

    // --- 3. Resto de funcionalidades (Guardar/Nuevo) ---
    $('#btn-nuevo').on('click', function () {
        $('#form-ordinador')[0].reset();
        $('#ord-id').val('');
        $('#modalOrdinador').modal('show');
    });

    $('#btn-actualizar').click(function () {
        // Carga inicial
        fetchAndRender(API_URL, $tbody, crearFilaOrdinador);
    });

    $('#btn-guardar').on('click', function () {
        const ordinadorData = {
            nom: $('#ord-nom').val(),
            serialNumber: $('#ord-serial').val(),
            type: $('#ord-type').val(),
            model: $('#ord-model').val(),
            ubicacio: $('#ord-ubicacio').val(),
            ram: $('#ord-ram').val(),
            hdd: $('#ord-hdd').val(),
            so: $('#ord-so').val(),
            estat: parseInt($('#ord-estat').val()),
            purchaseDate: $('#ord-date').val()
        };

        const id = $('#ord-id').val();
        const ajaxType = id ? 'PUT' : 'POST';
        const ajaxUrl = id ? `${API_URL}/${id}` : API_URL;

        $.ajax({
            url: ajaxUrl,
            method: ajaxType,
            contentType: 'application/json',
            data: JSON.stringify(ordinadorData),
            success: function () {
                $('#modalOrdinador').modal('hide');
                fetchAndRender(API_URL, $tbody, crearFilaOrdinador);
                alert('Guardado correctamente');
            }
        });
    });

    // Carga inicial
    fetchAndRender(API_URL, $tbody, crearFilaOrdinador);
});