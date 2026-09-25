[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
$url = "https://blogger.googleusercontent.com/img/b/R29vZ2xl/AVvXsEiSZzd8HFgvU5Nk3-5H6IM0X98Q2Of0MQDQxp8ZON5bAERd6elusg3ir--zNcaNFROzxywcIEuCfHDl6YWrzBTBOVlZxhyFB-pmqROr7cNpaC9oj8SbRn0tsgFz1vXzqxlQCqf3ZbGRvj-WVxPHeDn7TjMptG87hzvaN5mPI0psUUvcS16v1KR-Cawgltvw/s1254/ChatGPT%20Image%20May%2027,%202026,%2008_37_40%20PM.png"
$outputPath = "c:\Users\budia\Downloads\SIKAP\logo_custom.png"

Invoke-WebRequest -Uri $url -OutFile $outputPath -UserAgent "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"
Write-Host "Success downloading logo to $outputPath"
