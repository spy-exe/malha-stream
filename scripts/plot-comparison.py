"""Create a figure only from provided comparison data, never placeholder values."""
import argparse
import json
from pathlib import Path
import matplotlib.pyplot as plt

parser=argparse.ArgumentParser()
parser.add_argument('report',type=Path)
parser.add_argument('--out',type=Path,required=True)
args=parser.parse_args()
report=json.loads(args.report.read_text())
if report['status']!='BENCHMARKED' or not report['baseline']['n'] or not report['candidate']['n']:
    raise SystemExit('No comparable measured dataset available; figure not created')
values=[report['baseline']['p50_ms'],report['candidate']['p50_ms']]
figure,axis=plt.subplots(figsize=(6,4),layout='constrained')
axis.bar(['Baseline','Candidato'],values,color=['#606770','#167b68'])
axis.set_ylabel('Mediana (ms)')
axis.set_title(report['measurement']+' — dados fornecidos')
axis.axhline(0,color='black',linewidth=.5)
figure.text(.01,.01,'A figura não aprova melhora física; revisar método, incerteza e regressões.',fontsize=8)
args.out.parent.mkdir(parents=True,exist_ok=True)
figure.savefig(args.out,dpi=200)
