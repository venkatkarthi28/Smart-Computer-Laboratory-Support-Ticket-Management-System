# Phase 17 - analytics URLs
Adds GET /api/admin/analytics/by-category and /by-priority (2 new files, nothing overwritten).

1. Merge: unzip into the project root.
2. .\mvnw.cmd clean compile   -> BUILD SUCCESS
3. Restart the app, then in the second terminal (after defining Api/Login/Check, same as before):
     $adm = Login "admin@college.com" $seed
     $stu = Login "student@college.com" $seed
     $r = Api GET "/api/admin/analytics/by-category" $null $adm
     Check "by-category -> 200 with rows" (($r.code -eq 200) -and (@($r.json).Count -ge 1)) $r.code
     $r = Api GET "/api/admin/analytics/by-priority" $null $adm
     Check "by-priority -> 200 with rows" (($r.code -eq 200) -and (@($r.json).Count -ge 1)) $r.code
     $r = Api GET "/api/admin/analytics/by-category" $null $stu
     Check "student -> 403" ($r.code -eq 403) $r.code
     $r = Api GET "/api/admin/analytics/by-category"
     Check "no token -> 401" ($r.code -eq 401) $r.code
