import React, { useEffect } from "react";
import { Form, Input, Select, Button, notification } from "antd";
import { useAppSelector } from "@/redux/hooks";
import { callFetchAccount, callFetchUserById, callUpdateUser } from "@/config/api";

const ChangeInformation = () => {
    const [form] = Form.useForm();
    const user = useAppSelector((state) => state.account.user);

    useEffect(() => {
        const initData = async () => {
            const res = await callFetchUserById(user.id);
            if (res && res.data) {
                form.setFieldsValue(res.data);
            }
        }
        initData();
    }, [])

    const onFinish = async (values: any) => {
        const res = await callUpdateUser({ id: user.id, ...values });
        if (res && res.data) {
            notification.success({
                message: "Success",
                description: "User information updated successfully.",
            });
        } else {
            notification.error({
                message: "Error",
                description: "Failed to update user information.",
            });
        }
    };

    return (
        <Form
            form={form}
            layout="vertical"
            onFinish={onFinish}
            style={{ width: "600px", margin: "0 auto" }}
        >
            {/* Name */}
            <Form.Item
                label="Name"
                name="name"
                rules={[{ required: true, message: "Please enter your name" }]}
            >
                <Input placeholder="Enter name" />
            </Form.Item>

            {/* Address */}
            <Form.Item
                label="Address"
                name="address"
                rules={[{ required: true, message: "Please enter your address" }]}
            >
                <Input placeholder="Enter address" />
            </Form.Item>

            {/* Age */}
            <Form.Item
                label="Age"
                name="age"
                rules={[{ required: true, message: "Please enter age" }]}
            >
                <Input type="number" placeholder="Enter age" />
            </Form.Item>

            {/* Gender */}
            <Form.Item
                label="Gender"
                name="gender"
                rules={[{ required: true, message: "Please select gender" }]}
            >
                <Select placeholder="Select gender">
                    <Select.Option value="male">Male</Select.Option>
                    <Select.Option value="female">Female</Select.Option>
                    <Select.Option value="other">Other</Select.Option>
                </Select>
            </Form.Item>

            <Button type="primary" htmlType="submit" style={{ width: '100%' }}>
                Update
            </Button>
        </Form>
    );
};

export default ChangeInformation;
