signtool.exe sign /debug /sha1 2E6D5F676034FBE09DBB79C3DBCECB1E94D5A879 /t http://timestamp.digicert.com *.msi
signtool.exe verify /pa *.msi
