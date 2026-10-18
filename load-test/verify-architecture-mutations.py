#!/usr/bin/env python3
"""Temporarily inject production-source violations, prove failures, and always clean up."""
import json
from pathlib import Path
import subprocess

ROOT=Path(__file__).resolve().parents[1]
REPORT=ROOT/'docs/prd'
SOURCE=ROOT/'main-vpc/core/transfer/src/main/java'

def check(name,files,task,expected):
    created=[]
    try:
        for relative,code in files.items():
            path=SOURCE/relative
            if path.exists():
                raise RuntimeError(f'Refusing to overwrite existing source: {path}')
            path.parent.mkdir(parents=True,exist_ok=True)
            path.write_text(code)
            created.append(path)
        result=subprocess.run(['./gradlew',*task,'--no-daemon'],cwd=ROOT,text=True,capture_output=True)
        output=result.stdout+result.stderr
        # Test stdout is in the XML report, while Gradle itself may only print a failure summary.
        test_xml=ROOT/'main-vpc/core/transfer/build/test-results/test/TEST-com.shinhan.pg.transfer.ArchitectureTest.xml'
        evidence=output+(test_xml.read_text() if test_xml.exists() else '')
        (REPORT/f'verification-{name.lower()}-expected-failure.log').write_text(evidence)
        if result.returncode==0 or expected not in evidence:
            raise RuntimeError(f'{name}: expected failure evidence missing')
        return {'rule':name,'exit_code':result.returncode,'expected_failure_observed':True,'guard':expected}
    finally:
        for path in reversed(created):
            path.unlink(missing_ok=True)
            if path.parent.name=='account':
                try: path.parent.rmdir()
                except OSError: pass

def main():
    results=[]
    results.append(check('R1',{'com/shinhan/pg/transfer/domain/ArchitectureRuleViolation.java':
        'package com.shinhan.pg.transfer.domain; @org.springframework.stereotype.Component public class ArchitectureRuleViolation {}'},
        [':transfer:test','--tests','com.shinhan.pg.transfer.ArchitectureTest'],'R1:'))
    results.append(check('R5',{
        'com/shinhan/pg/account/DiagnosticForeign.java':'package com.shinhan.pg.account; public class DiagnosticForeign {}',
        'com/shinhan/pg/transfer/domain/ArchitectureRuleViolation.java':
        'package com.shinhan.pg.transfer.domain; import com.shinhan.pg.account.DiagnosticForeign; public class ArchitectureRuleViolation { DiagnosticForeign value; }'},
        [':transfer:test','--tests','com.shinhan.pg.transfer.ArchitectureTest'],'R5:'))
    results.append(check('R6',{'com/shinhan/pg/transfer/domain/ArchitectureRuleViolation.java':
        'package com.shinhan.pg.transfer.domain; public class ArchitectureRuleViolation extends com.shinhan.pg.common.commonmoney.Money {}'},
        [':transfer:compileJava'],'cannot inherit from final Money'))
    (REPORT/'verification-architecture-mutations.json').write_text(json.dumps(results,indent=2)+'\n')
    print(json.dumps(results,indent=2))

if __name__=='__main__':main()
