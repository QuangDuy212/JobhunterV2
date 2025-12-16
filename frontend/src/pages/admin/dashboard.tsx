import React, { useEffect } from 'react';
import { Card, Col, Row, Typography, Space, Button, Input, Divider, List, Tag, Result } from 'antd';
import { RiseOutlined, FileTextOutlined, TeamOutlined, BarChartOutlined, EnvironmentOutlined, HistoryOutlined } from '@ant-design/icons';

// --- Import từ Chart.js và react-chartjs-2 ---
import { Line, Bar, Doughnut } from 'react-chartjs-2';
import {
    Chart as ChartJS,
    CategoryScale,
    LinearScale,
    PointElement,
    LineElement,
    BarElement,
    ArcElement,
    Title,
    Tooltip,
    Legend
} from 'chart.js';
import { callCountAllJobs, callCountAllResumes, callCountAllUsers, callCountResumesByStatus, callCountResumesByTime, callFetchAllLog, callFetchAllSkill, callFetchJobsBySkill } from '@/config/api';
import { set } from 'lodash';
import { useAppSelector } from '@/redux/hooks';

// Đăng ký các thành phần cần thiết của Chart.js
ChartJS.register(
    CategoryScale,
    LinearScale,
    PointElement,
    LineElement,
    BarElement,
    ArcElement, // Dùng cho biểu đồ Doughnut
    Title,
    Tooltip,
    Legend
);

const { Title: AntdTitle, Text } = Typography;

// --- Dữ liệu và Cấu hình cho Chart.js ---




// 2. Job Postings by Department (Biểu đồ Cột - Bar Chart)

const barOptions = {
    responsive: true,
    plugins: {
        legend: { display: false },
        title: { display: false },
    },
    scales: {
        y: { beginAtZero: true, },
        x: {
            // Tối ưu hóa việc hiển thị tên cột
            ticks: {
                autoSkip: false, // Hiển thị tất cả nhãn
                maxRotation: 45, // Xoay nhãn tối đa 45 độ
                minRotation: 45, // Xoay nhãn tối thiểu 45 độ
            },
        }
    },
    // Thêm khoảng cách giữa các cột
    barPercentage: 0.5, // Độ rộng của cột (giảm giá trị để tăng khoảng cách)
    categoryPercentage: 0.7, // Độ rộng của nhóm cột
};




// --- Component Dashboard Chính (Giữ nguyên cấu trúc Antd) ---

interface StatCardProps {
    title: string;
    value: string;
    change: string;
    icon: React.ReactNode;
    iconBg: string;
    iconColor: string;
    valueColor?: string;
}

const StatCard: React.FC<StatCardProps> = ({ title, value, change, icon, iconBg, iconColor, valueColor = '#333' }) => (
    <Card bordered={false} style={{ height: '100%' }}>
        <Row justify="space-between" align="top">
            <Col>
                <div style={{ backgroundColor: iconBg, color: iconColor, padding: 8, borderRadius: '50%', display: 'inline-flex' }}>
                    {icon}
                </div>
            </Col>
            <Col>
                <Text strong style={{ color: change.includes('-') ? '#f5222d' : '#52c41a' }}>
                    {change}
                </Text>
            </Col>
        </Row>
        <div style={{ marginTop: 15 }}>
            <Text type="secondary" style={{ display: 'block' }}>{title}</Text>
            <AntdTitle level={3} style={{ margin: '4px 0 0', color: valueColor }}>{value}</AntdTitle>
        </div>
    </Card>
);

const DashboardPage = () => {

    const [countAllUsers, setCountAllUsers] = React.useState<number>(0);
    const [countAllResumes, setCountAllResumes] = React.useState<number>(0);
    const [countAllJobs, setCountAllJobs] = React.useState<number>(0);
    const [countAllCompanies, setCountAllCompanies] = React.useState<number>(0);
    const [resumesBy12Months, setResumesBy12Months] = React.useState<number[]>([]);
    const [jobsBySkill, setJobsBySkill] = React.useState<{ skill: string; count: number }[]>([]);
    const [listSkills, setListSkills] = React.useState<string[]>([]);
    const [auditLogs, setAuditLogs] = React.useState<any[]>([]);
    const [resumesByStatus, setResumesByStatus] = React.useState<{ status: string; count: number }[]>([]);
    const user = useAppSelector(state => state.account.user);

    const lineOptions = {
        responsive: true,
        plugins: {
            legend: { position: "bottom" },
            title: { display: false },
        },
        scales: {
            y: {
                // 1. Đặt phạm vi Min/Max (Dựa vào hình ảnh 0-120)
                min: 0,
                max: resumesBy12Months.length > 0 ? Math.max(...resumesBy12Months) + 20 : 120,

                // 2. Định nghĩa bước nhảy cố định (Mỗi bước nhảy 20)
                ticks: {
                    stepSize: 1, // Chart.js sẽ tự sinh ra 0, 20, 40, 60, ... 120
                },

                // Đảm bảo không bắt đầu từ 0 là đủ, nhưng min/max kiểm soát tốt hơn
                beginAtZero: true,
            },
        },
    };

    // 1. Applications & Hires Trend (Biểu đồ Đường - Line Chart)
    const trendLabels = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
    const trendData = {
        labels: trendLabels,
        datasets: [
            {
                label: 'Applications',
                borderColor: '#1890ff', // Màu xanh dương Ant Design
                backgroundColor: 'rgba(24, 144, 255, 0.1)',
                tension: 0.4, // Tạo đường cong mượt mà
                fill: false,
            },
            {
                label: 'Hires',
                data: resumesBy12Months,
                borderColor: '#52c41a', // Màu xanh lá Ant Design
                backgroundColor: 'rgba(82, 196, 26, 0.1)',
                tension: 0.4,
                fill: false,
            },
        ],
    };

    const jobPostingLabels = [...listSkills];
    const jobPostingData = {
        labels: jobPostingLabels,
        datasets: [
            {
                label: 'Count',
                data: [...jobsBySkill.map(item => item.count)],
                backgroundColor: '#1890ff',
            },
        ],
    };

    // 3. Application Status Distribution (Biểu đồ Donut - Doughnut Chart)
    const statusData = {
        labels: resumesByStatus.map(item => item.status),
        datasets: [
            {
                data: resumesByStatus.map(item => item.count), // Tổng 100%
                backgroundColor: [
                    '#faad14', // Vàng cam (Reviewing) <- MÀU THỨ TƯ ĐÃ ĐƯỢC THÊM
                    '#1890ff', // Xanh dương (Active)
                    '#52c41a', // Xanh lá (On Hold)
                    '#f5222d', // Đỏ (Closed)
                ],
                hoverBackgroundColor: [
                    '#ffc53d', // Màu hover tương ứng
                    '#40a9ff',
                    '#73d13d',
                    '#ff4d4f',
                ],
                borderWidth: 1,
            },
        ],
    };
    const doughnutOptions = {
        responsive: true,
        plugins: {
            legend: { position: 'right' },
            title: { display: false },
            tooltip: {
                callbacks: {
                    label: ({ label, raw }: { label: string; raw: number }) => `${label}: ${raw}%`
                }
            }
        },
        cutout: '70%', // Làm cho nó thành biểu đồ Donut thay vì Pie
    };


    useEffect(() => {
        fetchCountAllUsers();
        fetchCountAllResumes();
        fetchCountAllJobs();
        fetchCountAllCompanies();
        fetchResumesBy12Months();
        fetchJobsBySkill();
        fetchCountResumesByStatus();
        fetchAuditLogs();
        // 1. Tải dữ liệu ngay lập tức
        fetchAuditLogs();

        // 2. Thiết lập tự động tải lại mỗi 10 giây
        const intervalId = setInterval(() => {
            fetchAuditLogs();
        }, 10000); // 10000ms = 10 giây

        // 3. Dọn dẹp khi component unmount
        return () => clearInterval(intervalId);
    }, []);

    const fetchCountAllUsers = async () => {
        const res = await callCountAllUsers();
        if (res && res.data)
            setCountAllUsers(res.data);
    };

    const fetchCountAllResumes = async () => {
        const res = await callCountAllResumes();
        if (res && res.data)
            setCountAllResumes(res.data);
    }
    const fetchCountAllJobs = async () => {
        const res = await callCountAllJobs();
        if (res && res.data)
            setCountAllJobs(res.data);
    }

    const fetchCountAllCompanies = async () => {
        const res = await callCountAllJobs();
        if (res && res.data)
            setCountAllCompanies(res.data);
    }

    const fetchCountResumesByStatus = async () => {
        const res = await callCountResumesByStatus();
        if (res && res.data) {
            const total = res.data.map((item: any) => item.count).reduce((a: number, b: number) => a + b, 0);
            setResumesByStatus(res.data.map((item: any) => ({
                status: item.status,
                count: item.count / total * 100
            })));
        }
    }

    const fetchResumesBy12Months = async () => {
        const arr = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12];
        const times = arr.map(async (month) => {
            const res = await callCountResumesByTime(2025, month);
            if (res && res.data) {
                setResumesBy12Months(prev => {
                    const newArr = [...prev];
                    newArr[month - 1] = res.data ?? 0;
                    return newArr;
                });
            }
        })
    }

    const fetchJobsBySkill = async () => {
        const skills = await callFetchAllSkill(`page=1&size=100`);
        if (skills && skills.data && skills.data.result)
            skills.data.result.map(async (skill: any) => {
                const res = await callFetchJobsBySkill(skill.id);
                setListSkills(prev => {
                    if (prev.includes(skill.name)) {
                        return prev;
                    }
                    return [...prev, skill.name];
                });
                if (res && res.data) {
                    setJobsBySkill(prev => {
                        if (prev.findIndex(item => item.skill === skill.name) !== -1) {
                            return prev;
                        }
                        return [...prev, { skill: skill.name, count: res.data?.length ?? 0 }];
                    });
                }
            });
    }

    const getLogTag = (action: string) => {
        if (action.includes('DENIED') || action.includes('DELETE') || action.includes('REJECTED') || action.includes('FAILURE') || action.includes('ACCESS_DENIED')) {
            return <Tag color="red">{action}</Tag>;
        }
        if (action.includes('GRANTED') || action.includes('CREATE') || action.includes('APPROVED')) {
            return <Tag color="green">{action}</Tag>;
        }
        if (action.includes('LOGIN') || action.includes('REVIEWING') || action.includes('GET')) {
            return <Tag color="blue">{action}</Tag>;
        }
        return <Tag color="default">{action}</Tag>;
    };

    const fetchAuditLogs = async () => {
        const res = await callFetchAllLog();
        if (res && res.data) {
            setAuditLogs(res.data); // Lấy 10 log mới nhất
        }
    }
    return (
        <>
            {user.role.name === 'USER' ?
                <Result
                    status="403"
                    title="Truy cập bị từ chối"
                    subTitle="Xin lỗi, bạn không có quyền hạn (permission) truy cập thông tin này"
                />
                :
                <div style={{ padding: 24 }}>
                    {/* Header */}
                    <AntdTitle level={2} style={{ margin: '0 0 5px' }}>Dashboard</AntdTitle>
                    <Text type="secondary" style={{ display: 'block', marginBottom: 20 }}>Welcome back! Here's what's happening with your recruitment.</Text>

                    <Divider style={{ margin: '10px 0 20px 0' }} />



                    {/* Hàng 1: Overview Cards (Giữ nguyên Antd) */}
                    <Row gutter={[20, 20]} style={{ marginBottom: 20 }}>
                        {/* ... (Các StatCard khác) ... */}
                        <Col xs={24} sm={12} lg={6}>
                            <StatCard
                                title="Total Applications"
                                value={countAllResumes.toString()}
                                change="+12.5%"
                                icon={<FileTextOutlined style={{ fontSize: 16 }} />}
                                iconBg="#e6f7ff"
                                iconColor="#1890ff"
                            />
                        </Col>
                        <Col xs={24} sm={12} lg={6}>
                            <Card bordered={false} style={{ height: '100%', background: 'linear-gradient(135deg, #6dd5ed 0%, #2193b0 100%)' }}>
                                <Row justify="space-between" align="top">
                                    <Col>
                                        <div style={{ backgroundColor: 'rgba(255,255,255,0.2)', color: '#fff', padding: 8, borderRadius: '50%', display: 'inline-flex' }}>
                                            <BarChartOutlined style={{ fontSize: 16 }} />
                                        </div>
                                    </Col>
                                    <Col>
                                        <Text strong style={{ color: 'rgba(255,255,255,0.8)' }}>
                                            +8.2%
                                        </Text>
                                    </Col>
                                </Row>
                                <div style={{ marginTop: 15 }}>
                                    <Text style={{ display: 'block', color: 'rgba(255,255,255,0.8)' }}>Active Jobs</Text>
                                    <AntdTitle level={3} style={{ margin: '4px 0 0', color: '#fff' }}>{countAllJobs}</AntdTitle>
                                </div>
                            </Card>
                        </Col>
                        <Col xs={24} sm={12} lg={6}>
                            <StatCard
                                title="Total Users"
                                value={countAllUsers.toString()}
                                change="+5.1%"
                                icon={<TeamOutlined style={{ fontSize: 16 }} />}
                                iconBg="#fff0f6"
                                iconColor="#eb2f96"
                            />
                        </Col>
                        <Col xs={24} sm={12} lg={6}>
                            <StatCard
                                title="Toltal companies"
                                value={countAllCompanies.toString()}
                                change="-2.3%"
                                icon={<RiseOutlined style={{ fontSize: 16 }} />}
                                iconBg="#fff1f0"
                                iconColor="#f5222d"
                            />
                        </Col>
                    </Row>

                    {/* Hàng 4: Biểu đồ Donut (Dùng Chart.js) */}
                    <Row gutter={[20, 20]} style={{ marginBottom: 20 }}>
                        <Col xs={24} lg={12}>
                            <Card title={<AntdTitle level={4} style={{ margin: 0 }}>Application Status Distribution</AntdTitle>} bordered={false}>
                                <div style={{ maxWidth: 400, margin: '0 auto', padding: '20px 0' }}>
                                    <Doughnut options={doughnutOptions} data={statusData} />
                                </div>
                            </Card>
                        </Col>
                        {/* CỘT 2: LỊCH SỬ HOẠT ĐỘNG MỚI */}
                        <Col xs={24} lg={12}>
                            <Card
                                title={<AntdTitle level={4} style={{ margin: 0 }}>History</AntdTitle>}
                                bordered={false}
                                style={{ height: '100%' }} // Đảm bảo chiều cao đồng đều
                            >
                                {/* Wrapper để tạo thanh cuộn nếu nội dung quá dài */}
                                <div style={{ height: 410, overflowY: 'auto' }}>
                                    <List
                                        itemLayout="horizontal"
                                        dataSource={auditLogs} // Sử dụng state auditLogs (10 item mới nhất)
                                        renderItem={(item: any) => (
                                            <List.Item>
                                                <List.Item.Meta
                                                    avatar={<HistoryOutlined style={{ fontSize: '24px', color: '#1890ff', paddingTop: '4px' }} />}
                                                    title={
                                                        <Space size={4}>
                                                            {getLogTag(item.action)}
                                                            <Text strong>{item.performedBy}</Text>
                                                        </Space>
                                                    }
                                                    description={
                                                        <>
                                                            <Text style={{ fontSize: '16px' }}>{item.details}</Text>
                                                            <br />
                                                            <Text type="secondary" style={{ fontSize: '14px' }}>{new Date(item.timestamp).toLocaleString()}</Text>
                                                        </>
                                                    }
                                                />
                                            </List.Item>
                                        )}
                                    />
                                    {/* Hiển thị message nếu không có log */}
                                    {auditLogs.length === 0 && (
                                        <div style={{ textAlign: 'center', padding: '50px 0' }}>
                                            <Text type="secondary">Không có hoạt động gần đây nào.</Text>
                                        </div>
                                    )}
                                </div>
                            </Card>
                        </Col>
                    </Row>

                    {/* Hàng 3: Biểu đồ Đường và Cột (Dùng Chart.js) */}
                    <Row gutter={[20, 20]} style={{ marginBottom: 20 }}>
                        <Col xs={24} lg={24}>
                            <Card
                                title={<AntdTitle level={4} style={{ margin: 0 }}>Applications & Hires Trend</AntdTitle>}
                                bordered={false}
                                style={{  width: '100%' }} // Full màn hình
                            >
                                <div style={{ height: 'calc(100% - 50px)', width: '100%' }}> {/* Trừ chiều cao tiêu đề */}
                                    <Line options={lineOptions} data={trendData} />
                                </div>
                            </Card>
                        </Col>

                        <Col xs={24} lg={24}>
                            <Card
                                title={<AntdTitle level={4} style={{ margin: 0 }}>Job By Skill</AntdTitle>}
                                bordered={false}
                                style={{  width: '100%' }} // Full màn hình
                            >
                                <div style={{ overflowX: 'auto', height: 'calc(100% - 50px)', width: '100%' }}> {/* Cuộn ngang nếu cần */}
                                    <Bar options={barOptions} data={jobPostingData} />
                                </div>
                            </Card>
                        </Col>
                    </Row>


                </div>
            }
        </>
    );
};

export default DashboardPage;