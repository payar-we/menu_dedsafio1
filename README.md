# Hexasafio Menu (Fabric 1.21.1)

Reemplaza el menu principal de Minecraft por el de HEXASAFIO:
video de fondo + logo + botones JUGAR (entra a nexsafio4.mcsh.gg) / OPCIONES / SALIR.
Tambien cambia el menu de pausa (ESC): VOLVER AL JUEGO / OPCIONES / DESCONECTARSE.

## Como generar el .jar con GitHub
1. Crea un repositorio nuevo en GitHub (vacio).
2. Sube TODO el contenido de esta carpeta (son solo 23 archivos; incluye la carpeta oculta `.github`).
   - Mas facil: en la pagina del repo -> "Add file" -> "Upload files" -> arrastra todo.
   - Si el navegador no sube `.github`, crea el archivo a mano: "Add file" -> "Create new file"
     con el nombre `.github/workflows/build.yml` y pega el contenido.
3. Ve a la pestana **Actions**. Se ejecuta solo "Build mod" (tarda 2-4 min).
   Si no arranca: Actions -> Build mod -> "Run workflow".
4. Cuando termine en verde, entra a esa ejecucion y abajo en **Artifacts** descarga `hexasafio-menu`.
   Dentro esta `hexasafio-menu-1.0.0.jar`.
5. Ponlo en la carpeta `mods` (Minecraft 1.21.1 con Fabric Loader 0.16+).

## Cambiar cosas
- IP del servidor del boton JUGAR: `src/main/java/com/hexasafio/HexaTitleScreen.java` (arriba, `SERVER_IP`).
- Imagenes: `src/main/resources/assets/hexasafio/textures/gui/` (logo, estudio = cabecera hexagon.studio, jugar, opciones, salir; p_* = botones del menu de pausa).
  Si cambias el tamano en pixeles de un PNG, actualiza las constantes `*_W` / `*_H` en `HexaTitleScreen.java`.
- Video: esta empaquetado en `assets/hexasafio/video/frames.bin` (1 solo archivo).
