# A separate, disposable PC emulator. Never connects to a TV or uses the default ADB server.
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$avdHome = 'G:\Android\mihrab-emulator-phase1\avd'
$avdConfig = Join-Path $avdHome 'Mihrab_Phase1.avd'
$emulatorData = 'G:\Android\mihrab-emulator-phase1'
New-Item -ItemType Directory -Force $avdConfig, $emulatorData, (Join-Path $emulatorData 'home') | Out-Null
@"
avd.ini.encoding=UTF-8
path=$avdConfig
target=android-37
"@ | Set-Content -LiteralPath (Join-Path $avdHome 'Mihrab_Phase1.ini') -Encoding ascii
@"
AvdId=Mihrab_Phase1
avd.ini.displayname=Mihrab local visual QA
abi.type=x86_64
hw.cpu.arch=x86_64
hw.cpu.ncore=2
hw.ramSize=1536
hw.lcd.width=1920
hw.lcd.height=1080
hw.lcd.density=320
hw.mainKeys=yes
hw.keyboard=yes
hw.gpu.enabled=yes
hw.gpu.mode=swiftshader
disk.dataPartition.size=2G
image.sysdir.1=system-images\android-37.1\google_apis_playstore_ps16k\x86_64\
tag.id=google_apis_playstore
PlayStore.enabled=true
showDeviceFrame=no
skin.name=1920x1080
"@ | Set-Content -LiteralPath (Join-Path $avdConfig 'config.ini') -Encoding ascii
$env:ANDROID_AVD_HOME=$avdHome
$env:ANDROID_EMULATOR_HOME=Join-Path $emulatorData 'home'
$env:ANDROID_SDK_ROOT='G:\Android\SDK'
$env:ANDROID_ADB_SERVER_PORT='5041'
$env:ADB_SERVER_SOCKET='tcp:127.0.0.1:5041'
$env:ADB_MDNS_AUTO_CONNECT='0'
& 'G:\Android\SDK\platform-tools\adb.exe' -P 5041 start-server
$arguments=@('-avd','Mihrab_Phase1','-datadir',$emulatorData,'-port','5580','-no-window','-no-audio','-no-boot-anim','-no-snapshot','-gpu','swiftshader','-memory','1536','-no-metrics','-no-direct-adb')
$process=Start-Process -FilePath 'G:\Android\SDK\emulator\emulator.exe' -ArgumentList $arguments -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $projectRoot 'validation\emulator.stdout.log') -RedirectStandardError (Join-Path $projectRoot 'validation\emulator.stderr.log')
Write-Output "Local emulator process: $($process.Id); isolated ADB port 5041; only serial emulator-5580."
