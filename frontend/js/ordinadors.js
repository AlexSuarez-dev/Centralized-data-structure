import { API_BASE, ESTATS_ACTIU, escapeHtml, errorMessage, fetchAndRender, fillEstatSelect } from './utils.js';

$(document).ready(function () {
    const API_URL = `${API_BASE}/ordinadors`;
    const $tbody = $('#tabla-body');
    // Initialize Bootstrap 5 tooltips in a delegated way for dynamic content
    new bootstrap.Tooltip(document.getElementById('tabla-body'), {
        selector: '[data-bs-toggle="tooltip"]'
    });

    fillEstatSelect($('#ord-estat'), ESTATS_ACTIU);

    // --- 1. Renderer logic specific to computers ---
    function crearFilaOrdinador(item) {
        const fecha = item.purchaseDate || 'Sin fecha';
        // --- Notes logic ---
        let obsHtml = '<span class="text-muted">-</span>';
        if (item.observacions && item.observacions.trim() !== '') {
            // If there are notes, create an icon with a Bootstrap tooltip
            obsHtml = `
            <i class="bi bi-info-circle-fill text-primary"
               data-bs-toggle="tooltip"
               data-bs-placement="top"
               title="${escapeHtml(item.observacions)}"
               style="cursor: help; font-size: 1.1rem;">
            </i>`;
        }

        const estat = ESTATS_ACTIU[item.estat] || { label: item.estat || '?', badge: 'bg-light text-dark', text: '' };

        // Package the whole object into an HTML attribute safely
        const itemData = encodeURIComponent(JSON.stringify(item));

        return `
            <tr>
                <td>
                    <strong class="${estat.text}">${escapeHtml(item.nom)}</strong><br>
                    <small class="text-muted">${escapeHtml(item.model)}</small>
                </td>
                <td>
                    <span class="badge bg-light text-dark border">${escapeHtml(item.type)}</span>
                    <span class="badge ${estat.badge}">${estat.label}</span>
                </td>
                <td><small>${escapeHtml(item.serialNumber)}</small></td>
                <td>${escapeHtml(item.ubicacio)}</td>
                <td>
                    <div style="font-size: 0.85rem;">
                        <strong>SO:</strong> ${escapeHtml(item.so || 'N/A')}<br>
                        <strong>RAM:</strong> ${escapeHtml(item.ram || 'N/A')} | <strong>HDD:</strong> ${escapeHtml(item.hdd || 'N/A')}
                    </div>
                </td>
                <td><small>${escapeHtml(fecha)}</small></td>
                <td class="text-center">${obsHtml}</td>
                <td class="text-center">
                    <button class="btn btn-sm btn-outline-primary btn-editar" data-item="${itemData}">
                        <i class="bi bi-pencil"></i>
                    </button>
                </td>
            </tr>`;
    }


    $tbody.on('click', '.btn-editar', function () {
        // 1. Extract and decode the button data
        const item = JSON.parse(decodeURIComponent($(this).data('item')));

        // 2. Fill all form fields
        $('#ord-id').val(item.id);
        $('#ord-nom').val(item.nom);
        $('#ord-serial').val(item.serialNumber);
        $('#ord-type').val(item.type);
        $('#ord-model').val(item.model);
        $('#ord-ubicacio').val(item.ubicacio);
        $('#ord-ram').val(item.ram);
        $('#ord-hdd').val(item.hdd);
        $('#ord-so').val(item.so);
        $('#ord-estat').val(item.estat);
        $('#ord-date').val(item.purchaseDate);
        $('#ord-observacions').val(item.observacions);

        // 3. Change the modal title so the user knows what it does
        $('#modalTitle').text('Editar Equipo: ' + item.nom);

        // 4. Show the modal
        $('#modalOrdinador').modal('show');
    });

    // --- 2. Search event with debounce (fires while typing) ---
    let timer;
    $('#inventorySearch').on('input', function () {
        const query = $(this).val();
        clearTimeout(timer);
        timer = setTimeout(() => {
            fetchAndRender(API_URL, $tbody, crearFilaOrdinador, query);
        }, 300); // Wait 300ms before calling the backend
    });

    // --- 3. Remaining functionality (Save/New) ---
    $('#btn-nuevo').on('click', function () {
        $('#form-ordinador')[0].reset();
        $('#ord-id').val('');
        $('#modalTitle').text('Nuevo Ordenador');
        $('#modalOrdinador').modal('show');
    });

    $('#btn-actualizar').click(function () {
        fetchAndRender(API_URL, $tbody, crearFilaOrdinador, $('#inventorySearch').val());
    });

    $('#btn-guardar').on('click', function () {
        const ordinadorData = {
            id: $('#ord-id').val(),
            nom: $('#ord-nom').val(),
            serialNumber: $('#ord-serial').val(),
            type: $('#ord-type').val(),
            model: $('#ord-model').val(),
            ubicacio: $('#ord-ubicacio').val(),
            ram: $('#ord-ram').val(),
            hdd: $('#ord-hdd').val(),
            so: $('#ord-so').val(),
            estat: $('#ord-estat').val(),
            purchaseDate: $('#ord-date').val() || null, // Set to null if empty
            // PUT replaces every field: if notes were not sent, they would be wiped
            observacions: $('#ord-observacions').val()
        };

        const id = ordinadorData.id;
        const ajaxType = id == '' ? 'POST' : 'PUT';
        const ajaxUrl = id && id != '' ? `${API_URL}/${id}` : API_URL;


        $.ajax({
            url: ajaxUrl,
            method: ajaxType,
            contentType: 'application/json',
            data: JSON.stringify(ordinadorData),
            success: function () {
                $('#modalOrdinador').modal('hide');
                fetchAndRender(API_URL, $tbody, crearFilaOrdinador, $('#inventorySearch').val());

                Swal.fire({
                    icon: 'success',
                    title: '¡Guardado!',
                    text: 'El equipo se ha guardado correctamente.',
                    toast: true,           // Turns it into a small notification
                    position: 'top-end',   // Top-right position
                    showConfirmButton: false,
                    timer: 3000,           // Disappears after 3 seconds
                    timerProgressBar: true
                });
            },
            error: function (xhr, status, error) {
                console.error('Error al guardar:', error);

                Swal.fire({
                    icon: 'error',
                    title: '¡Vaya!',
                    text: errorMessage(xhr, 'Error al guardar el ordenador. Por favor, inténtelo de nuevo.'),
                    confirmButtonColor: '#0d6efd' // Bootstrap blue color
                });
            }
        });
    });

    // Initial load
    fetchAndRender(API_URL, $tbody, crearFilaOrdinador);
});
