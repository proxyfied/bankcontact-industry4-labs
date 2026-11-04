"""Extract only allowlisted error codes; never export provider bodies or credentials."""
import json,re,sys
from pathlib import Path
log=Path(sys.argv[1])
text=log.read_text(errors='replace') if log.exists() else ''
codes=['insufficient_quota','invalid_api_key','model_not_found','billing_hard_limit_reached','rate_limit_exceeded','unsupported_country_region_territory','account_deactivated']
result={'provider_error_codes':[code for code in codes if code in text],
        'http_statuses':sorted(set(re.findall(r'(?:HTTP\s+|status(?: code)?[:= ]+)([45]\d\d)',text,re.I)))}
out=Path(sys.argv[2]);out.parent.mkdir(parents=True,exist_ok=True);out.write_text(json.dumps(result,indent=2))
print(json.dumps(result))
