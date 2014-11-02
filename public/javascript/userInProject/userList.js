function changeStatus(userId, status) {
    var $change = $("#change-status");
    $change.find('input[name="userId"]').val(userId);
    $change.find('input[name="status"]').val(status);
    $change.submit();
}