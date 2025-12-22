import { Breadcrumb, Col, ConfigProvider, Divider, Form, Row, message, notification } from "antd";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { DebounceSelect } from "../user/debouce.select";
import { FooterToolbar, ProForm, ProFormDatePicker, ProFormDigit, ProFormSelect, ProFormSwitch, ProFormText } from "@ant-design/pro-components";
import styles from 'styles/admin.module.scss';
import { LOCATION_LIST } from "@/config/utils";
import { ICompanySelect } from "../user/modal.user";
import { useState, useEffect } from 'react';
import { callCreateJob, callFetchAllSkill, callFetchCompany, callFetchJobById, callUpdateJob } from "@/config/api";
import ReactQuill from 'react-quill';
import 'react-quill/dist/quill.snow.css';
import { CheckSquareOutlined } from "@ant-design/icons";
import enUS from 'antd/lib/locale/en_US';
import dayjs from 'dayjs';
import { IJob, ISkill } from "@/types/backend";
import { JobStatusEnum } from "@/constant/common.enum";

interface ISkillSelect {
    label: string;
    value: string;
    key?: string;
}

const ViewUpsertJob = (props: any) => {
    const [companies, setCompanies] = useState<ICompanySelect[]>([]);
    const [skills, setSkills] = useState<ISkillSelect[]>([]);
    const navigate = useNavigate();
    const [value, setValue] = useState<string>("");
    let location = useLocation();
    let params = new URLSearchParams(location.search);
    const id = params?.get("id");
    const [dataUpdate, setDataUpdate] = useState<IJob | null>(null);
    const [form] = Form.useForm();

    useEffect(() => {
        const init = async () => {
            const temp = await fetchSkillList();
            setSkills(temp);
            if (id) {
                const res = await callFetchJobById(id);
                if (res && res.data) {
                    setDataUpdate(res.data);
                    setValue(res.data.description);
                    setCompanies([{
                        label: res.data.company?.name as string,
                        value: `${res.data.company?.id}@#$${res.data.company?.logo}` as string,
                        key: res.data.company?.id
                    }]);
                    const skillTemp: any = res.data?.skills?.map((item: ISkill) => ({
                        label: item.name,
                        value: item.id,
                        key: item.id
                    }));
                    form.setFieldsValue({
                        ...res.data,
                        company: {
                            label: res.data.company?.name as string,
                            value: `${res.data.company?.id}@#$${res.data.company?.logo}` as string,
                            key: res.data.company?.id
                        },
                        skills: skillTemp
                    });
                }
            }
        };
        init();
        return () => form.resetFields();
    }, [id]);

    async function fetchCompanyList(name: string): Promise<ICompanySelect[]> {
        const res = await callFetchCompany(`page=1&size=100&name ~ '${name}'`);
        if (res && res.data) {
            return res.data.result.map(item => ({
                label: item.name as string,
                value: `${item.id}@#$${item.logo}` as string
            }));
        } else return [];
    }

    async function fetchSkillList(): Promise<ISkillSelect[]> {
        const res = await callFetchAllSkill(`page=1&size=100`);
        if (res && res.data) {
            return res.data.result.map(item => ({
                label: item.name as string,
                value: `${item.id}` as string
            }));
        } else return [];
    }

    // Hàm bôi đậm từ vi phạm
    const highlightViolatedWords = (content: string, words: string[]) => {
        let newContent = content;
        words.forEach(word => {
            const regex = new RegExp(`(${word})`, 'gi');
            newContent = newContent.replace(regex, '<span style="color: red; background-color: yellow; font-weight: bold;">$1</span>');
        });
        return newContent;
    };

    const onFinish = async (values: any) => {
        const cp = values?.company?.value?.split('@#$');
        let arrSkills = [];
        if (dataUpdate?.id) {
            arrSkills = typeof values?.skills?.[0] === 'object' 
                ? values?.skills?.map((item: any) => ({ id: item.value }))
                : values?.skills?.map((item: any) => ({ id: +item }));
        } else {
            arrSkills = values?.skills?.map((item: string) => ({ id: +item }));
        }

        const job = {
            name: values.name,
            skills: arrSkills,
            company: {
                id: cp && cp.length > 0 ? cp[0] : "",
                name: values.company.label,
                logo: cp && cp.length > 1 ? cp[1] : ""
            },
            location: values.location,
            salary: values.salary,
            quantity: values.quantity,
            level: values.level,
            description: value, // Lấy từ state ReactQuill
            startDate: /[0-9]{2}[/][0-9]{2}[/][0-9]{4}$/.test(values.startDate) ? dayjs(values.startDate, 'DD/MM/YYYY').toDate() : values.startDate,
            endDate: /[0-9]{2}[/][0-9]{2}[/][0-9]{4}$/.test(values.endDate) ? dayjs(values.endDate, 'DD/MM/YYYY').toDate() : values.endDate,
            active: values.active,
        }

        const res = dataUpdate?.id 
            ? await callUpdateJob(job, dataUpdate.id)
            : await callCreateJob(job);

        if (res.data && !res.error) {
            message.success(dataUpdate?.id ? "Cập nhật job thành công" : "Tạo mới job thành công");
            navigate('/admin/job');
        } else {
            // Xử lý lỗi AI
            if (res.error === "AI_CONTENT_REJECTED" && Array.isArray(res.data)) {
                const highlighted = highlightViolatedWords(value, res.data);
                setValue(highlighted);
                notification.error({
                    message: 'Vi phạm nội dung',
                    description: `AI phát hiện từ ngữ không phù hợp: ${res.data.join(", ")}. Vui lòng sửa lại vùng được bôi đỏ.`,
                    duration: 8
                });
            } else {
                notification.error({
                    message: 'Có lỗi xảy ra',
                    description: res.message
                });
            }
        }
    };

    return (
        <div className={styles["upsert-job-container"]}>
            <div className={styles["title"]}>
                <Breadcrumb separator=">" items={[{ title: <Link to="/admin/job">Manage Job</Link> }, { title: 'Upsert Job' }]} />
            </div>
            <ConfigProvider locale={enUS}>
                <ProForm
                    form={form}
                    onFinish={onFinish}
                    submitter={{
                        searchConfig: { resetText: "Cancel", submitText: <>{dataUpdate?.id ? "Cập nhật Job" : "Tạo mới Job"}</> },
                        onReset: () => navigate('/admin/job'),
                        render: (_: any, dom: any) => <FooterToolbar>{dom}</FooterToolbar>,
                        submitButtonProps: { icon: <CheckSquareOutlined /> },
                    }}
                >
                    <Row gutter={[20, 20]}>
                        <Col span={24} md={12}><ProFormText label="Tên Job" name="name" rules={[{ required: true, message: 'Bắt buộc' }]} placeholder="Nhập tên job" /></Col>
                        <Col span={24} md={6}><ProFormSelect name="skills" label="Kỹ năng" options={skills} mode="multiple" rules={[{ required: true }]} /></Col>
                        <Col span={24} md={6}><ProFormSelect name="location" label="Địa điểm" options={LOCATION_LIST.filter(item => item.value !== 'ALL')} rules={[{ required: true }]} /></Col>
                        <Col span={24} md={6}><ProFormDigit label="Mức lương" name="salary" rules={[{ required: true }]} fieldProps={{ addonAfter: " đ", formatter: (v) => `${v}`.replace(/\B(?=(\d{3})+(?!\d))/g, ',') }} /></Col>
                        <Col span={24} md={6}><ProFormDigit label="Số lượng" name="quantity" rules={[{ required: true }]} /></Col>
                        <Col span={24} md={6}><ProFormSelect name="level" label="Trình độ" valueEnum={{ INTERN: 'INTERN', FRESHER: 'FRESHER', JUNIOR: 'JUNIOR', MIDDLE: 'MIDDLE', SENIOR: 'SENIOR' }} rules={[{ required: true }]} /></Col>
                        {(dataUpdate?.id || !id) && (
                            <Col span={24} md={6}>
                                <ProForm.Item name="company" label="Thuộc Công Ty" rules={[{ required: true }]}>
                                    <DebounceSelect showSearch placeholder="Chọn công ty" fetchOptions={fetchCompanyList} style={{ width: '100%' }} />
                                </ProForm.Item>
                            </Col>
                        )}
                        <Col span={24} md={6}><ProFormDatePicker label="Ngày bắt đầu" name="startDate" rules={[{ required: true }]} fieldProps={{ format: 'DD/MM/YYYY' }} /></Col>
                        <Col span={24} md={6}><ProFormDatePicker label="Ngày kết thúc" name="endDate" rules={[{ required: true }]} fieldProps={{ format: 'DD/MM/YYYY' }} /></Col>
                        {(!dataUpdate?.id || (dataUpdate?.id && !(dataUpdate.status === JobStatusEnum.REJECTED))) && (
                            <Col span={24} md={6}><ProFormSwitch label="Active" name="active" initialValue={true} /></Col>
                        )}
                        <Col span={24}>
                            <ProForm.Item name="description" label="Miêu tả job" rules={[{ required: true }]}>
                                <ReactQuill theme="snow" value={value} onChange={setValue} />
                            </ProForm.Item>
                        </Col>
                    </Row>
                    <Divider />
                </ProForm>
            </ConfigProvider>
        </div>
    );
}

export default ViewUpsertJob;