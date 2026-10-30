# LightUtils — Minecraft Forge 1.20.1

Mod para **Minecraft 1.20.1 + Forge 47.x**, compilado con **Java 17**.

## Compilar localmente

Necesitas Java 17 y Gradle 8.8:

```bash
gradle --no-daemon build
```

El JAR se genera en `build/libs/`.

## Compilar en GitHub Actions

1. Sube el contenido de esta carpeta a la raíz de un repositorio de GitHub.
2. Abre la pestaña **Actions**.
3. El workflow **Build Forge mod** se ejecuta automáticamente al hacer `push` o abrir un `pull_request`. También puedes iniciarlo con **Run workflow**.
4. Cuando termine, abre la ejecución y descarga el artefacto `lightutils-forge-1.20.1` desde **Artifacts**.

## Comandos

Requieren nivel de permiso OP 2:

```text
/lightutils add <color> <intensidad 0-15> <radio 1-64> <tag>
/lightutils clear <tag>
```

Colores admitidos: `rojo`, `verde`, `azul`, `amarillo`, `naranja`, `morado`, `rosa`, `cian`, `blanco` o un color hexadecimal de 6 dígitos (por ejemplo `ff8800`).
