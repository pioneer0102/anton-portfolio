# クラウドデプロイメント アーキテクチャ設計書

## 概要

本文書は、FastAPI ToDo管理アプリケーションをクラウド環境にデプロイする際のシステム構成について、AWS及びGCPでの具体的なサービス名とコンポーネント名を用いて説明します。

## アプリケーション要件分析

### 技術スタック
- **フレームワーク**: FastAPI (Python)
- **データベース**: SQLite3 (開発環境) → PostgreSQL/MySQL (本番環境)
- **認証**: API Token認証 (X-API-TOKEN ヘッダー)
- **API エンドポイント**: REST API (CRUD操作)

### 非機能要件
- **可用性**: 99.9%以上
- **セキュリティ**: HTTPS通信、API認証
- **スケーラビリティ**: オートスケーリング対応
- **モニタリング**: ログ収集、メトリクス監視
- **バックアップ**: データベースの定期バックアップ

## AWS デプロイメント アーキテクチャ

### 1. コンピューティング層

#### Amazon ECS (Fargate)
```
Application Load Balancer (ALB)
├── Target Group (Port 8000)
└── ECS Service
    ├── Task Definition (FastAPI Container)
    │   ├── CPU: 512, Memory: 1024MB
    │   ├── Port Mapping: 8000
    │   └── Environment Variables
    ├── Auto Scaling (2-10 tasks)
    └── Health Check (/health-check)
```

**代替案: Amazon EKS**
```
Application Load Balancer (ALB)
├── AWS Load Balancer Controller
└── EKS Cluster
    ├── Node Group (t3.medium)
    ├── Deployment (FastAPI pods)
    ├── Service (ClusterIP)
    ├── Horizontal Pod Autoscaler
    └── Ingress Resource
```

### 2. データベース層

#### Amazon RDS (PostgreSQL)
```
RDS PostgreSQL
├── Engine: PostgreSQL 15.x
├── Instance Class: db.t3.micro (開発) / db.r5.large (本番)
├── Multi-AZ Deployment (本番環境)
├── Automated Backup (7日間保持)
├── Read Replica (読み取り負荷分散)
└── Security Group (Port 5432)
```

### 3. ネットワーク層

#### Amazon VPC
```
VPC (10.0.0.0/16)
├── Public Subnet 1a (10.0.1.0/24)
├── Public Subnet 1c (10.0.2.0/24)
├── Private Subnet 1a (10.0.10.0/24)
├── Private Subnet 1c (10.0.20.0/24)
├── Internet Gateway
├── NAT Gateway (Public Subnet)
└── Route Tables
```

#### セキュリティグループ
```
ALB Security Group
├── Inbound: HTTPS (443) from 0.0.0.0/0
├── Inbound: HTTP (80) from 0.0.0.0/0
└── Outbound: 8000 to ECS Security Group

ECS Security Group
├── Inbound: 8000 from ALB Security Group
└── Outbound: 5432 to RDS Security Group

RDS Security Group
├── Inbound: 5432 from ECS Security Group
└── Outbound: None
```

### 4. セキュリティ・認証層

#### AWS Certificate Manager (ACM)
- SSL/TLS証明書の自動発行・更新
- ALBでのHTTPS終端

#### AWS WAF
- SQLインジェクション防止
- DDoS攻撃対策
- 地理的制限

#### AWS Secrets Manager
- データベース認証情報の管理
- API キーの安全な保存

### 5. 監視・ログ層

#### Amazon CloudWatch
```
CloudWatch Logs
├── ECS Log Group (/ecs/fastapi-app)
├── ALB Access Logs
└── RDS Logs

CloudWatch Metrics
├── ECS Service Metrics
├── ALB Metrics
├── RDS Performance Insights
└── Custom Application Metrics

CloudWatch Alarms
├── High CPU Usage (> 80%)
├── High Memory Usage (> 80%)
├── Database Connection Errors
└── HTTP 5xx Error Rate
```

#### AWS X-Ray
- 分散トレーシング
- パフォーマンス分析

### 6. CI/CDパイプライン

#### AWS CodePipeline
```
Source Stage (GitHub/CodeCommit)
├── Build Stage (CodeBuild)
│   ├── Docker Image Build
│   ├── Unit Tests
│   └── Push to ECR
└── Deploy Stage
    ├── ECS Service Update
    └── Blue/Green Deployment
```

### 7. コスト概算 (月額・東京リージョン)

#### 開発環境
- ECS Fargate: $20-30
- RDS db.t3.micro: $15-20
- ALB: $20
- 合計: 約$55-70

#### 本番環境
- ECS Fargate (2-4 tasks): $60-120
- RDS db.r5.large (Multi-AZ): $300-400
- ALB + WAF: $40-50
- 合計: 約$400-570

## GCP デプロイメント アーキテクチャ

### 1. コンピューティング層

#### Google Cloud Run
```
Cloud Load Balancer (HTTPS)
├── Backend Service
└── Cloud Run Service
    ├── Container Image (FastAPI)
    ├── CPU: 1, Memory: 2Gi
    ├── Concurrency: 100
    ├── Min Instances: 1, Max: 10
    └── Health Check (/health-check)
```

**代替案: Google Kubernetes Engine (GKE)**
```
Cloud Load Balancer
├── Ingress Controller
└── GKE Cluster
    ├── Node Pool (e2-medium)
    ├── Deployment (FastAPI pods)
    ├── Service (LoadBalancer)
    └── Horizontal Pod Autoscaler
```

### 2. データベース層

#### Cloud SQL (PostgreSQL)
```
Cloud SQL PostgreSQL
├── Version: PostgreSQL 15
├── Machine Type: db-f1-micro (開発) / db-n1-standard-2 (本番)
├── High Availability (本番環境)
├── Automated Backup (7日間保持)
├── Read Replica
└── Private IP (VPC Peering)
```

### 3. ネットワーク層

#### Virtual Private Cloud (VPC)
```
VPC Network (custom)
├── Subnet tokyo (10.0.1.0/24)
├── Subnet osaka (10.0.2.0/24)
├── Cloud NAT
├── Cloud Router
└── Firewall Rules
```

#### Firewall Rules
```
allow-https-http
├── Direction: Ingress
├── Targets: http-server tag
├── Source: 0.0.0.0/0
└── Ports: 80, 443

allow-cloud-run
├── Direction: Ingress
├── Targets: Cloud Run services
├── Source: Load Balancer ranges
└── Ports: 8080
```

### 4. セキュリティ層

#### Google Cloud Armor
- DDoS攻撃防止
- WAFルール適用
- 地理的制限

#### Secret Manager
- データベース認証情報
- API キーの管理

#### SSL証明書
- Google-managed SSL certificates
- 自動更新

### 5. 監視・ログ層

#### Cloud Logging
```
Log Router
├── Cloud Run Logs
├── Load Balancer Logs
├── Cloud SQL Logs
└── Audit Logs
```

#### Cloud Monitoring
```
Metrics
├── Cloud Run Metrics (CPU, Memory, Requests)
├── Load Balancer Metrics
├── Cloud SQL Performance
└── Custom Metrics

Alerting Policies
├── High Error Rate (> 5%)
├── High Latency (> 1s)
├── Database Connection Issues
└── Low Instance Count
```

#### Cloud Trace
- 分散トレーシング
- レイテンシ分析

### 6. CI/CDパイプライン

#### Cloud Build
```
Trigger (GitHub)
├── Build Step
│   ├── Docker Build
│   ├── Unit Tests
│   └── Push to Artifact Registry
└── Deploy Step
    └── Cloud Run Deploy
```

### 7. コスト概算 (月額・東京リージョン)

#### 開発環境
- Cloud Run: $10-20
- Cloud SQL db-f1-micro: $10-15
- Load Balancer: $20
- 合計: 約$40-55

#### 本番環境
- Cloud Run (scaled): $50-100
- Cloud SQL db-n1-standard-2 (HA): $200-300
- Load Balancer + Cloud Armor: $30-40
- 合計: 約$280-440

## 推奨アーキテクチャの選択指針

### AWS vs GCP 比較

| 観点 | AWS | GCP |
|------|-----|-----|
| **コスト** | 高め | 比較的安価 |
| **運用性** | ECS管理が複雑 | Cloud Runがシンプル |
| **スケーラビリティ** | 高い | 高い |
| **サービス成熟度** | 非常に高い | 高い |
| **国内事例** | 多い | 増加中 |

### 推奨構成

#### 小規模・スタートアップ向け
**GCP + Cloud Run + Cloud SQL**
- 理由: シンプル、安価、運用が容易

#### 大規模・エンタープライズ向け
**AWS + EKS + RDS**
- 理由: 豊富な機能、高い可用性、成熟したエコシステム

## セキュリティ考慮事項

### 1. データ保護
- データベース暗号化 (保存時・転送時)
- API Token の安全な管理
- 個人情報の適切な取り扱い

### 2. アクセス制御
- IAM ロールベースのアクセス制御
- ネットワークレベルの分離
- API Gateway での認証・認可

### 3. 監査・コンプライアンス
- アクセスログの保持
- 監査証跡の記録
- GDPR/CCPA対応

## 災害復旧・事業継続計画

### 1. バックアップ戦略
- データベースの自動バックアップ (日次)
- アプリケーションコードのバージョン管理
- インフラ設定のコード化 (IaC)

### 2. 可用性設計
- マルチAZ配置
- ロードバランサによる負荷分散
- ヘルスチェックによる自動復旧

### 3. 復旧手順
- RTO (Recovery Time Objective): 1時間以内
- RPO (Recovery Point Objective): 1時間以内
- 復旧手順書の整備

## 運用・保守計画

### 1. モニタリング項目
- アプリケーションレスポンス時間
- エラー率
- リソース使用率
- データベースパフォーマンス

### 2. アラート設定
- 障害発生時の即座な通知
- エスカレーション手順
- 対応チームの連絡体制

### 3. 定期メンテナンス
- セキュリティパッチ適用
- データベース最適化
- 不要データの削除

## 面接でよく聞かれる質問と回答例

### Q1: なぜこのアーキテクチャを選択したのか？
**A**: アプリケーションの特性（FastAPI、SQLite）を考慮し、マネージドサービスを活用することで運用コストを削減し、スケーラビリティを確保できるため。

### Q2: セキュリティの課題と対策は？
**A**: API Token認証、HTTPS通信、WAF導入、データベース暗号化により多層防御を実現。

### Q3: コスト最適化の方法は？
**A**: Auto Scaling、Reserved Instance、適切なインスタンスサイズ選択、不要リソースの定期削除。

### Q4: 障害時の対応手順は？
**A**: ヘルスチェック、Multi-AZ配置、自動復旧機能により可用性を確保。監視アラートによる迅速な障害検知。

### Q5: 将来的な拡張性は？
**A**: マイクロサービス化、CDN導入、キャッシュ層追加、データベース分割などが可能。

## まとめ

本アプリケーションのクラウドデプロイメントでは、以下の点が重要です：

1. **シンプルさ**: マネージドサービスの活用
2. **セキュリティ**: 多層防御の実装
3. **スケーラビリティ**: 負荷に応じた自動拡張
4. **可用性**: 障害に強い構成
5. **コスト効率**: 適切なリソース選択

GCPのCloud RunとCloud SQLの組み合わせが、本アプリケーションには最も適していると考えられます。
