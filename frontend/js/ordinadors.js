import { fetchAndRender } from './utils.js';

$(document).ready(function () {
    const API_URL = 'http://svharmonia:8080/api/v1/ordinadors';
    const $tbody = $('#tabla-body');
    // Inicializar Tooltips de Bootstrap 5 de forma delegada para contenido dinámico
    new bootstrap.Tooltip(document.getElementById('tabla-body'), {
        selector: '[data-bs-toggle="tooltip"]'
    });

    // --- 1. Lógica de renderizado específico para Ordenadores ---
    function crearFilaOrdinador(item) {
        const fecha = item.purchaseDate || 'Sin fecha';
        // --- Lógica de las notas ---
        let obsHtml = '<span class="text-muted">-</span>';
        if (item.observacions && item.observacions.trim() !== '') {
            // Si hay notas, creamos un icono con el Tooltip de Bootstrap
            obsHtml = `
            <i class="bi bi-info-circle-fill text-primary" 
               data-bs-toggle="tooltip" 
               data-bs-placement="top" 
               title="${item.observacions}" 
               style="cursor: help; font-size: 1.1rem;">
            </i>`;
        }

        let colorClase = '';
        if (item.estat == 1) colorClase = 'text-success';
        else if (item.estat == 2) colorClase = 'text-warning';
        else colorClase = 'text-danger';

        // Empaquetamos todo el objeto en un atributo HTML de forma segura
        const itemData = encodeURIComponent(JSON.stringify(item));

        return `
            <tr>
                <input type="hidden" value="${item.id}">
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
                <td class="text-center">${obsHtml}</td>
                <td class="text-center">
                    <button class="btn btn-sm btn-outline-primary btn-editar" data-item="${itemData}">
                        <i class="bi bi-pencil"></i>
                    </button>
                </td>
            </tr>`;
    }


    $tbody.on('click', '.btn-editar', function () {
        // 1. Extraer y desencriptar los datos del botón
        const item = JSON.parse(decodeURIComponent($(this).data('item')));

        // 2. Rellenar todos los campos del formulario
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

        // 3. Cambiar el título del Modal para que el usuario sepa qué hace
        $('#modalTitle').text('Editar Equipo: ' + item.nom);

        // 4. Mostrar el Modal
        $('#modalOrdinador').modal('show');
    });

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
        console.log($('#ord-id').val());
        
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
            estat: parseInt($('#ord-estat').val()),
            purchaseDate: $('#ord-date').val()
        };

        console.log(ordinadorData);
        
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
                fetchAndRender(API_URL, $tbody, crearFilaOrdinador);
                
                // --- NUEVA ALERTA VISUAL DE ÉXITO ---
                Swal.fire({
                    icon: 'success',
                    title: '¡Guardado!',
                    text: 'El equipo se ha guardado correctamente.',
                    toast: true,           // Lo convierte en una notificación pequeña
                    position: 'top-end',   // Arriba a la derecha
                    showConfirmButton: false,
                    timer: 3000,           // Desaparece sola a los 3 segundos
                    timerProgressBar: true
                });
            },
            error: function (xhr, status, error) {
                console.error('Error al guardar:', error);                
                
                // --- NUEVA ALERTA VISUAL DE ERROR ---
                Swal.fire({
                    icon: 'error',
                    title: '¡Vaya!',
                    text: 'Error al guardar el ordenador. Por favor, inténtelo de nuevo.',
                    confirmButtonColor: '#0d6efd' // Color azul de Bootstrap
                });
            }
        });
    });

    // Carga inicial
    fetchAndRender(API_URL, $tbody, crearFilaOrdinador);
});