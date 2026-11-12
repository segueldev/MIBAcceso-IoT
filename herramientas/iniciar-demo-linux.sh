#!/usr/bin/env bash
set -euo pipefail
proyecto_mib=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
auxiliar_mib="$(dirname -- "$proyecto_mib")/.herramientas"
sdk_mib="${ANDROID_HOME:-$auxiliar_mib/android-sdk}"
avd_mib="$auxiliar_mib/avd"
apk_mib="$proyecto_mib/app/build/outputs/apk/debug/app-debug.apk"
if [[ ! -x "$sdk_mib/emulator/emulator" || ! -d "$avd_mib/MIB_Base.avd" || ! -d "$avd_mib/MIB_Agente.avd" ]]; then
    echo 'No se encontraron los dos emuladores preparados en este computador.'
    echo 'Crea dos dispositivos desde Device Manager de Android Studio y sigue el README.'
    exit 1
fi
if [[ ! -f "$apk_mib" ]]; then
    echo 'Compila la app desde Android Studio antes de iniciar la demostración.'
    exit 1
fi
adb_mib="$sdk_mib/platform-tools/adb"
for especificacion_mib in MIB_Base:5554 MIB_Agente:5556; do
    nombre_mib=${especificacion_mib%:*}
    puerto_mib=${especificacion_mib#*:}
    serial_mib="emulator-$puerto_mib"
    if ! "$adb_mib" -s "$serial_mib" get-state >/dev/null 2>&1; then
        ANDROID_AVD_HOME="$avd_mib" ANDROID_HOME="$sdk_mib" nohup "$sdk_mib/emulator/emulator" \
            -avd "$nombre_mib" -port "$puerto_mib" -gpu swiftshader -no-snapshot \
            -no-boot-anim -camera-back none -camera-front none \
            >"/tmp/$nombre_mib.log" 2>&1 &
    fi
    echo "Esperando a $nombre_mib..."
    listo_mib=false
    for intento_mib in $(seq 1 120); do
        if [[ $("$adb_mib" -s "$serial_mib" shell service check package 2>/dev/null) == *found* ]]; then
            listo_mib=true
            break
        fi
        sleep 2
    done
    if [[ "$listo_mib" != true ]]; then
        echo "El emulador no terminó de iniciar. Revisa /tmp/$nombre_mib.log"
        exit 1
    fi
    "$adb_mib" -s "$serial_mib" install -r "$apk_mib"
    "$adb_mib" -s "$serial_mib" shell am start -n cl.seguel.mib/.MainActivity
    "$adb_mib" -s "$serial_mib" shell settings put system screen_off_timeout 1800000
done
"$adb_mib" -s emulator-5554 forward tcp:8988 tcp:8988
echo 'Los dos Android están listos. El primero es la Base MIB; el segundo, el Agente.'
echo 'Crea tus credenciales, inicia la base, comparte su código y conecta al agente con 10.0.2.2.'
echo 'Las instrucciones completas están en el README del proyecto.'
