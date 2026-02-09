$resPath = "e:\2ndScannerConverter\app\src\main\res"
$xmlFiles = Get-ChildItem -Path $resPath -Recurse -Filter *.xml

foreach ($file in $xmlFiles) {
    try {
        $content = Get-Content -Path $file.FullName -Raw -Encoding Byte
        if ($content -contains 0) {
            Write-Output "CORRUPTED (Null bytes): $($file.FullName)"
        }
        else {
            # Try parsing as XML to check for syntax errors
            try {
                $xmlContent = [xml](Get-Content -Path $file.FullName -Raw)
            }
            catch {
                Write-Output "INVALID XML SYNTAX: $($file.FullName)"
                Write-Output $_.Exception.Message
            }
        }
    }
    catch {
        Write-Output "ERROR READING: $($file.FullName)"
    }
}
