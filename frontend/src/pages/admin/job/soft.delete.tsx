import DataTable from "@/components/client/data-table";
import { useAppDispatch, useAppSelector } from "@/redux/hooks";
import { IJob } from "@/types/backend";
import { ClockCircleOutlined, DeleteOutlined, EditOutlined, PlusOutlined } from "@ant-design/icons";
import { ActionType, ProColumns, ProFormSelect } from '@ant-design/pro-components';
import { Button, Popconfirm, Space, Tag, message, notification } from "antd";
import { useRef } from 'react';
import dayjs from 'dayjs';
import { callDeleteJob } from "@/config/api";
import queryString from 'query-string';
import { useNavigate } from "react-router-dom";
import Access from "@/components/share/access";
import { ALL_PERMISSIONS } from "@/config/permissions";
import { sfIn } from "spring-filter-query-builder";
import { JobStatusEnum } from "@/constant/common.enum";
import { fetchJobDeleted } from "@/redux/slice/jobDeletedSlide";

const JobDeleted = () => {
    const tableRef = useRef<ActionType>();

    const isFetching = useAppSelector(state => state.jobDeleted.isFetching);
    const meta = useAppSelector(state => state.jobDeleted.meta);
    const jobs = useAppSelector(state => state.jobDeleted.result);
    const dispatch = useAppDispatch();
    const navigate = useNavigate();

    const handleRestoreJob = async (id: string | undefined) => {
        if (id) {
            const res = await callDeleteJob(id);
            if (res.statusCode === 200) {
                message.success('Xóa Job thành công');
                reloadTable();
            } else {
                notification.error({
                    message: 'Error occur',
                    description: res.message
                });
            }
        }
    }

    const reloadTable = () => {
        tableRef?.current?.reload();
    }

    const getStatusTag = (status: any) => {
        let color = 'default';
        switch (status) {
            case JobStatusEnum.REVIEWING:
                color = 'gold'; // Màu vàng cho đang duyệt
                break;
            case JobStatusEnum.APPROVED:
                color = 'green'; // Màu xanh lá cho đã duyệt
                break;
            case JobStatusEnum.REJECTED:
                color = 'red'; // Màu đỏ cho bị từ chối
                break;
            case JobStatusEnum.INACTIVE:
                color = 'gray'; // Màu xám cho không hoạt động
                break;
            case JobStatusEnum.DRAFT:
                color = 'blue'; // Màu xanh dương cho bản nháp
                break;
        }
        return { color, label: status };
    };

    const columns: ProColumns<IJob>[] = [
        {
            title: 'No',
            key: 'index',
            width: 50,
            align: "center",
            render: (text, record, index) => {
                return (
                    <>
                        {(index + 1) + (meta.page - 1) * (meta.pageSize)}
                    </>)
            },
            hideInSearch: true,
        },
        {
            title: 'Name',
            dataIndex: 'name',
            sorter: true,
        },
        {
            title: 'Company',
            dataIndex: ["company", "name"],
            sorter: true,
            hideInSearch: true,
        },
        {
            title: 'Quanlity',
            dataIndex: 'salary',
            sorter: true,
            render(dom, entity, index, action, schema) {
                const str = "" + entity.salary;
                return <>{str?.replace(/\B(?=(\d{3})+(?!\d))/g, ',')} đ</>
            },
        },
        {
            title: 'Level',
            dataIndex: 'level',
            renderFormItem: (item, props, form) => (
                <ProFormSelect
                    showSearch
                    mode="multiple"
                    allowClear
                    valueEnum={{
                        INTERN: 'INTERN',
                        FRESHER: 'FRESHER',
                        JUNIOR: 'JUNIOR',
                        MIDDLE: 'MIDDLE',
                        SENIOR: 'SENIOR',
                    }}
                    placeholder="Chọn level"
                />
            ),
        },
        {
            title: 'Active',
            dataIndex: 'active',
            render(dom, entity, index, action, schema) {
                return <>
                    <Tag color={entity.active ? "lime" : "red"} >
                        {entity.active ? "ACTIVE" : "INACTIVE"}
                    </Tag>
                </>
            },
            hideInSearch: true,
        },

        {
            title: 'Status',
            dataIndex: 'status',
            render(dom, entity, index, action, schema) {
                return <>
                    <Tag color={getStatusTag(entity.status).color}>
                        {entity.status}
                    </Tag>
                </>
            },
            hideInSearch: true,
        },

        {
            title: 'CreatedAt',
            dataIndex: 'createdAt',
            width: 200,
            sorter: true,
            render: (text, record, index, action) => {
                return (
                    <>{record.createdAt ? dayjs(record.createdAt).format('DD-MM-YYYY HH:mm:ss') : ""}</>
                )
            },
            hideInSearch: true,
        },
        {
            title: 'UpdatedAt',
            dataIndex: 'updatedAt',
            width: 200,
            sorter: true,
            render: (text, record, index, action) => {
                return (
                    <>{record.updatedAt ? dayjs(record.updatedAt).format('DD-MM-YYYY HH:mm:ss') : ""}</>
                )
            },
            hideInSearch: true,
        },
        {

            title: 'Actions',
            hideInSearch: true,
            width: 50,
            render: (_value, entity, _index, _action) => (
                <Space>

                    < Access
                        permission={ALL_PERMISSIONS.USERS.RESTORE}
                        hideChildren
                    >
                        <ClockCircleOutlined
                            style={{
                                fontSize: 20,
                                color: '#ffa500',
                            }}
                            type=""
                            onClick={() => { handleRestoreJob(entity.id) }} />
                    </Access >
                </Space >
            ),

        },
    ];

    const buildQuery = (params: any, sort: any, filter: any) => {

        const clone = { ...params };
        let parts = [];
        if (clone.name) parts.push(`name ~ '${clone.name}'`);
        if (clone.salary) parts.push(`salary ~ '${clone.salary}'`);
        if (clone?.level?.length) {
            parts.push(`${sfIn("level", clone.level).toString()}`);
        }

        clone.filter = parts.join(' and ');
        if (!clone.filter) delete clone.filter;

        clone.page = clone.current;
        clone.size = clone.pageSize;

        delete clone.current;
        delete clone.pageSize;
        delete clone.name;
        delete clone.salary;
        delete clone.level;

        let temp = queryString.stringify(clone);

        let sortBy = "";
        const fields = ["name", "salary", "createdAt", "updatedAt"];
        if (sort) {
            for (const field of fields) {
                if (sort[field]) {
                    sortBy = `sort=${field},${sort[field] === 'ascend' ? 'asc' : 'desc'}`;
                    break;  // Remove this if you want to handle multiple sort parameters
                }
            }
        }

        //mặc định sort theo updatedAt
        if (Object.keys(sortBy).length === 0) {
            temp = `${temp}&sort=updatedAt,desc`;
        } else {
            temp = `${temp}&${sortBy}`;
        }

        return temp;
    }

    return (
        <div>
            <Access
                permission={ALL_PERMISSIONS.JOBS.GET_PAGINATE}
            >
                <DataTable<IJob>
                    actionRef={tableRef}
                    headerTitle="List Jobs"
                    rowKey="id"
                    loading={isFetching}
                    columns={columns}
                    dataSource={jobs}
                    request={async (params, sort, filter): Promise<any> => {
                        const query = buildQuery(params, sort, filter);
                        dispatch(fetchJobDeleted({ query }))
                    }}
                    scroll={{ x: true }}
                    pagination={
                        {
                            current: meta.page,
                            pageSize: meta.pageSize,
                            showSizeChanger: true,
                            total: meta.total,
                            showTotal: (total, range) => { return (<div> {range[0]}-{range[1]} per {total} rows</div>) }
                        }
                    }
                    rowSelection={false}
                />
            </Access>
        </div >
    )
}

export default JobDeleted;