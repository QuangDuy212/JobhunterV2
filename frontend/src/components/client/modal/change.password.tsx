import React from 'react';
import { Form, Input, Button, message } from 'antd';
import { LockOutlined } from '@ant-design/icons';
import { callChangePassword } from '@/config/api';

const ChangePasswordForm = () => {
  const [form] = Form.useForm();

  const onFinish = async (values: {currentPassword: string, newPassword : string}) => {
    console.log(">>>> check values: ", values);
    const res = await callChangePassword(values.currentPassword, values.newPassword);
    if(res.statusCode === 200){
        message.success('Đổi mật khẩu thành công!');
        form.resetFields();
    }else{
        message.error( res.message);
    }
  };

  return (
    <Form
      form={form}
      name="minimal_change_password"
      onFinish={onFinish}
      layout="vertical"
      style={{ maxWidth: 400, margin: '0 auto' }} // Căn giữa và giới hạn chiều rộng (tùy chọn)
    >
      {/* 1. Mật khẩu cũ */}
      <Form.Item
        name="currentPassword"
        label="Mật khẩu cũ"
        rules={[
          {
            required: true,
            message: 'Vui lòng nhập Mật khẩu cũ!',
          },
        ]}
      >
        <Input.Password 
          prefix={<LockOutlined />} 
          placeholder="Nhập mật khẩu cũ" 
        />
      </Form.Item>

      {/* 2. Mật khẩu mới */}
      <Form.Item
        name="newPassword"
        label="Mật khẩu mới"
        rules={[
          {
            required: true,
            message: 'Vui lòng nhập Mật khẩu mới!',
          },
          {
            min: 6,
            message: 'Mật khẩu phải có ít nhất 6 ký tự!',
          },
        ]}
        hasFeedback
      >
        <Input.Password 
          prefix={<LockOutlined />} 
          placeholder="Nhập mật khẩu mới" 
        />
      </Form.Item>

      {/* 3. Xác nhận mật khẩu mới */}
      <Form.Item
        name="confirmPassword"
        label="Xác nhận Mật khẩu mới"
        dependencies={['newPassword']}
        hasFeedback
        rules={[
          {
            required: true,
            message: 'Vui lòng xác nhận Mật khẩu mới!',
          },
          // Custom validator: So sánh hai mật khẩu
          ({ getFieldValue }) => ({
            validator(_, value) {
              if (!value || getFieldValue('newPassword') === value) {
                return Promise.resolve();
              }
              return Promise.reject(new Error('Hai mật khẩu đã nhập không khớp!'));
            },
          }),
        ]}
      >
        <Input.Password 
          prefix={<LockOutlined />} 
          placeholder="Xác nhận mật khẩu mới" 
        />
      </Form.Item>

      {/* Nút Submit */}
      <Form.Item>
        <Button 
          type="primary" 
          htmlType="submit" 
          style={{ width: '100%' }}
        >
          Đổi Mật Khẩu
        </Button>
      </Form.Item>
    </Form>
  );
};

export default ChangePasswordForm;