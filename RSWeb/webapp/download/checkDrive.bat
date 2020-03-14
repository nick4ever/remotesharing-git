:checkNetworkDrive
echo "Disconnecting M drive if mapped..."
net use M: /d

timeout /t 1

echo "Mapping M drive..."
net use M: \\192.168.88.200\Detailing /persistent:yes

echo "Checking if M drive is mapped..."
net use M: | findstr "Status" | findstr "OK">nul && (
    echo "M drive has been mapeed!!!"
    echo "Start opening the path..."
    timeout /t 1
    start " " "M:"
) || (
    goto :checkNetworkDrive
)
