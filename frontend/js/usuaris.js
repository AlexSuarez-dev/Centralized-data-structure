import { fetchAndRender } from './utils.js';

$(document).ready(function () {
    const API_URL = 'http://svharmonia:8080/api/v1/usuaris';
    const $tbody = $('#tabla-body');
    
    new bootstrap.Tooltip(document.getElementById('tabla-body'), {
        selector: '[data-bs-toggle="tooltip"]'
    });

    // --- 1. Lógica de renderizado específico para Usuarios ---
    function crearFilaUsuario(item) {
        // Lógica de las notas
        let obsHtml = '<span class="text-muted">-</span>';
        if (item.observacions && item.observacions.trim() !== '') {
            obsHtml = `
            <i class="bi bi-info-circle-fill text-primary" 
               data-bs-toggle="tooltip" 
               data-bs-placement="top" 
               title="${item.observacions}" 
               style="cursor: help; font-size: 1.1rem;">
            </i>`;
        }

        // Extraer datos del equipo asignado (si tiene)
        const equipoNom = item.actiu ? item.actiu.nom : '<span class="text-muted">Sin asignar</span>';
        const equipoSerie = item.actiu && item.actiu.serialNumber ? item.actiu.serialNumber : '-';
        
        // Datos que aún no están en BBDD (simulados por ahora)
        const email = item.email || '-';
        const depto = item.departament || '-';
        const kensington = item.kensington || '<span class="text-muted">No asignada</span>';
        const telefon = item.telefon || '-';

        const itemData = encodeURIComponent(JSON.stringify(item));

        return `
            <tr>
                <td><strong>${item.nom}</strong></td>
                <td>${telefon}</td>
                <td>
                    <div style="font-size: 0.85rem;">
                        <strong>@:</strong> ${email}<br>
                        <strong>Dpt:</strong> ${depto}
                    </div>
                </td>
                <td><span class="badge bg-primary">${equipoNom}</span></td>
                <td><small>${equipoSerie}</small></td>
                <td><code>${kensington}</code></td>
                <td class="text-center">${obsHtml}</td>
                <td class="text-center">
                    <button class="btn btn-sm btn-outline-primary btn-editar" data-item="${itemData}">
                        <i class="bi bi-pencil"></i>
                    </button>
                </td>
            </tr>`;
    }

    // --- 2. Evento Editar ---
    $tbody.on('click', '.btn-editar', function () {
        const item = JSON.parse(decodeURIComponent($(this).data('item')));

        // Como el "nom" es la Primary Key, lo guardamos en el hidden para el PUT
        $('#usr-original-id').val(item.nom);
        
        // Rellenar campos
        $('#usr-nom').val(item.nom);
        $('#usr-telefon').val(item.telefon);
        $('#usr-email').val(item.email);
        $('#usr-depto').val(item.departament);
        $('#usr-kensington').val(item.kensington);
        $('#usr-observacions').val(item.observacions);
        $('#usr-estat').val(item.estat || 1);
        
        if(item.actiu) {
            $('#usr-actiu-id').val(item.actiu.id);
        } else {
            $('#usr-actiu-id').val('');
        }

        // Si estamos editando, NO deberíamos poder cambiar el ID (Nombre)
        $('#usr-nom').prop('readonly', true);
        $('#modalTitle').text('Editar Usuario: ' + item.nom);
        $('#modalUsuario').modal('show');
    });

    // --- 3. Evento Nuevo Usuario ---
    $('#btn-nuevo').on('click', function () {
        $('#form-usuario')[0].reset();
        $('#usr-original-id').val('');
        $('#usr-nom').prop('readonly', false); // Aquí sí podemos escribir el nombre
        $('#modalTitle').text('Nuevo Usuario');
        $('#modalUsuario').modal('show');
    });

    $('#btn-actualizar').click(function () {
        fetchAndRender(API_URL, $tbody, crearFilaUsuario);
    });

    // --- 4. Evento Guardar (Debes tener tu DTO preparado en Java para recibir esto) ---
    $('#btn-guardar').on('click', function () {
        const originalId = $('#usr-original-id').val();
        
        // Construimos el objeto para enviar al backend
        const usuarioData = {
            nom: $('#usr-nom').val(),
            telefon: $('#usr-telefon').val() ? parseInt($('#usr-telefon').val()) : null,
            email: $('#usr-email').val(),
            departament: $('#usr-depto').val(),
            kensington: $('#usr-kensington').val(),
            observacions: $('#usr-observacions').val(),
            estat: parseInt($('#usr-estat').val()),
            // Enviaremos solo el ID del activo para que el backend lo asocie
            actiuId: $('#usr-actiu-id').val() ? parseInt($('#usr-actiu-id').val()) : null 
        };

        const ajaxType = originalId === '' ? 'POST' : 'PUT';
        // Si es PUT, la URL necesita el ID (el nombre original)
        const ajaxUrl = originalId !== '' ? `${API_URL}/${encodeURIComponent(originalId)}` : API_URL;

        $.ajax({
            url: ajaxUrl,
            method: ajaxType,
            contentType: 'application/json',
            data: JSON.stringify(usuarioData),
            success: function () {
                $('#modalUsuario').modal('hide');
                fetchAndRender(API_URL, $tbody, crearFilaUsuario);
                
                Swal.fire({
                    icon: 'success',
                    title: '¡Guardado!',
                    text: 'El usuario se ha guardado correctamente.',
                    toast: true,
                    position: 'top-end',
                    showConfirmButton: false,
                    timer: 3000,
                    timerProgressBar: true
                });
            },
            error: function (xhr, status, error) {
                console.error('Error al guardar:', error);                
                Swal.fire({
                    icon: 'error',
                    title: 'Error de guardado',
                    text: 'No se ha podido procesar el usuario.',
                    confirmButtonColor: '#0d6efd'
                });
            }
        });
    });

    // Búsqueda con Debounce
    let timer;
    $('#userSearch').change(function () {
        const query = $(this).val();
        clearTimeout(timer);
        timer = setTimeout(() => {
            fetchAndRender(API_URL, $tbody, crearFilaUsuario, query);
        }, 300);
    });

    // Carga inicial
    fetchAndRender(API_URL, $tbody, crearFilaUsuario);
});