/** Error con código HTTP; su mensaje se devuelve tal cual al cliente como { message }. */
class ErrorHttp extends Error {
  constructor(status, message) {
    super(message);
    this.status = status;
  }
}
const invalido = (m) => new ErrorHttp(400, m);
const noAutenticado = (m = 'Credenciales inválidas.') => new ErrorHttp(401, m);
const prohibido = (m = 'No tienes permiso para esta operación.') => new ErrorHttp(403, m);
const noEncontrado = (m = 'Registro no encontrado.') => new ErrorHttp(404, m);
const conflicto = (m) => new ErrorHttp(409, m);

/** Traduce errores de PostgreSQL a mensajes comprensibles. */
function desdePg(e) {
  switch (e && e.code) {
    case '23505': return conflicto('Ya existe un registro con esos datos.');
    case '23503': return conflicto('La operación no es posible: hay registros relacionados (o el registro referenciado no existe).');
    case '23514': return invalido('Los datos no cumplen las restricciones de la base de datos.');
    case '22003': return invalido('Un valor numérico excede el máximo permitido.');
    case '22P02': return invalido('Un valor tiene un formato inválido.');
    default: return null;
  }
}

module.exports = { ErrorHttp, invalido, noAutenticado, prohibido, noEncontrado, conflicto, desdePg };
